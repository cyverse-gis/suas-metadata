package controller;

import com.thebuzzmedia.exiftool.core.StandardTag;
import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;
import library.EditCell;
import library.ImageViewPane;
import library.TreeViewAutomatic;
import model.SanimalData;
import model.constant.SanimalDataFormats;
import model.image.*;
import model.location.Location;
import model.neon.jsonPOJOs.Site;
import model.species.Species;
import model.threading.ErrorTask;
import model.util.FXMLLoaderUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.StatusBar;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Controller class for the main import window
 */
public class SanimalImportController implements Initializable
{
	///
	/// FXML bound fields start
	///

	// The pane containing the image preview
	@FXML
	public ImageViewPane imagePreviewPane;

	// The image view to be contained inside the image view pane
	@FXML
	public ImageView imagePreview;

	// The stack pane containing the image preview
	@FXML
	public StackPane imagePane;

	// The tree view containing all the images and folders
	@FXML
	public TreeViewAutomatic<ImageContainer> imageTree;

	// The button to begin importing images
	@FXML
	public Button btnImportImages;
	// The button to delete imported images
	@FXML
	public Button btnDelete;

	// The region that hovers over the image which is used for its border
	@FXML
	public Region imageAddOverlay;

	// Status bar for showing how far we have completed metadata image tasks
	@FXML
	public StatusBar sbrTaskProgress;

	// Left and right arrow buttons to allow easy next and previous image selection
	@FXML
	public Button btnLeftArrow;
	@FXML
	public Button btnRightArrow;

	// The main pane holding everything
	@FXML
	public SplitPane mainPane;

	@FXML
	public TableView<MetadataEntry> metadataTableView;
	@FXML
	public TextField txtMetadataSearch;
	@FXML
	public Button btnResetSearch;

	@FXML
	public ListView<Site> lvwSites;

	///
	/// FXML bound fields end
	///

	// Fields to hold the currently selected image entry and image directory
	private ObjectProperty<ImageEntry> currentlySelectedImage = new SimpleObjectProperty<>(null);
	private ObjectProperty<ImageDirectory> currentlySelectedDirectory = new SimpleObjectProperty<>(null);
	// Use fade transitions to fade the species list in and out
	private FadeTransition fadeAddPanelIn;
	private FadeTransition fadeAddPanelOut;
	private FadeTransition fadeLeftIn;
	private FadeTransition fadeLeftOut;
	private FadeTransition fadeRightIn;
	private FadeTransition fadeRightOut;
	// A property used to process the image scrolling
	private ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

	/**
	 * Initialize the sanimal import view and data bindings
	 *
	 * @param ignored   ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL ignored, ResourceBundle resources)
	{
		// Initialize root of the right side directory/image tree and make the root invisible
		// This is because a treeview must have ONE root.

		// Create a fake invisible root node whos children
		final TreeItem<ImageContainer> ROOT = new TreeItem<>(SanimalData.getInstance().getImageTree());
		// Hide the fake invisible root
		this.imageTree.setShowRoot(false);
		// Set the fake invisible root
		this.imageTree.setRoot(ROOT);
		// Set the items of the tree to be the children of the fake invisible root
		this.imageTree.setItems(SanimalData.getInstance().getImageTree().getChildren());
		// Setup the image tree cells so that when they get drag & dropped the species & locations can be tagged
		this.imageTree.setCellFactory(x -> FXMLLoaderUtils.loadFXML("importView/ImageTreeCell.fxml").getController());
		// If we select a node that's being uploaded clear the selection
		this.imageTree.setOnKeyPressed(event ->
		{
			// If we're moving up or down on the tree, ensure we're not entering a disabled node
			if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN)
			{
				// Grab the selected tree node
				TreeItem<ImageContainer> selectedItem = this.imageTree.getSelectionModel().getSelectedItem();
				if (selectedItem != null)
				{
					// Grab the next node
					TreeItem<ImageContainer> next = event.getCode() == KeyCode.UP ? selectedItem.previousSibling() : selectedItem.nextSibling();
					// Make sure the next node has a value
					if (next != null && next.getValue() != null)
					{
						// Grab the next tree entries image container
						ImageContainer value = next.getValue();
						// If the image container is a directory being uploaded consome the key press
						if (value instanceof ImageDirectory && ((ImageDirectory) value).getUploadProgress() != -1)
						{
							event.consume();
						}
					}
				}
			}
		});

		// Bind the species entry location name to the selected image's location

		// When a new image is selected... we perform a bunch of actions below
		MonadicBinding<ImageContainer> selectedImage = EasyBind.monadic(this.imageTree.getSelectionModel().selectedItemProperty()).map(TreeItem::getValue);
		// Update the currently selected image and directory
		currentlySelectedImage.bind(selectedImage.map(imageContainer -> (imageContainer instanceof ImageEntry) ? (ImageEntry) imageContainer : null));
		currentlySelectedDirectory.bind(selectedImage.map(imageContainer -> (imageContainer instanceof ImageDirectory) ? (ImageDirectory) imageContainer : null));

		// When we select a cloud image or directory, don't allow clicking delete
		this.btnDelete.disableProperty().bind(
				Bindings.or(Bindings.createBooleanBinding(() -> this.currentlySelectedImage.getValue() instanceof CloudImageEntry, this.currentlySelectedImage),
				Bindings.or(Bindings.createBooleanBinding(() -> this.currentlySelectedDirectory.getValue() instanceof CloudImageDirectory, this.currentlySelectedDirectory),
							this.imageTree.getSelectionModel().selectedIndexProperty().isEqualTo(-1))));

		// Create bindings in the GUI
		// Finally bind the date taken's disable property if an adjustable image is selected
		//this.txtDateTaken.textProperty().bind(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::dateTakenProperty).map(localDateTime -> SanimalData.getInstance().getSettings().formatDateTime(localDateTime, " at ")).orElse(""));
		// Bind the image preview to the selected image from the right side tree view
		// Can't use 'new Image(file.toURI().toString(), SanimalData.getInstance().getSettings().getBackgroundImageLoading())));'
		// because it doesn't support tiffs. Sad day.
		this.imagePreview.imageProperty().bind(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::getFileProperty).map(file ->
		{
			try
			{
				return SwingFXUtils.toFXImage(ImageIO.read(file), null);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return null;
		}));
		this.imagePreview.imageProperty().addListener((observable, oldValue, newValue) -> this.resetImageView(null));
		// Hide the progress bar when no tasks remain
		this.sbrTaskProgress.visibleProperty().bind(SanimalData.getInstance().getSanimalExecutor().getQueuedExecutor().taskRunningProperty());
		// Bind the progress bar's text property to tasks remaining
		this.sbrTaskProgress.textProperty().bind(SanimalData.getInstance().getSanimalExecutor().getQueuedExecutor().messageProperty());
		// Bind the progress bar's progress property to the current task's progress
		this.sbrTaskProgress.progressProperty().bind(SanimalData.getInstance().getSanimalExecutor().getQueuedExecutor().progressProperty());
		// Bind the left arrow's visibility property to if there is a previous item available
		this.btnLeftArrow.visibleProperty().bind(
				this.imageTree.getSelectionModel().selectedIndexProperty()
						// -1 Would mean that nothing is selected
						.isEqualTo(-1)
						// If 0 is selected, that means we're at the top element
						.or(this.imageTree.getSelectionModel().selectedIndexProperty()
								.isEqualTo(0))
						// Make sure to negate because we want to hide the arrow when the above things are true
						.not());
		// Bind the left arrow's visibility property to if there is a next item available
		this.btnRightArrow.visibleProperty().bind(
				this.imageTree.getSelectionModel().selectedIndexProperty()
						// Expanded item count property counts the total number of entries, so the last one is count - 1. If this is selected hide the right arrow
						.isEqualTo(this.imageTree.expandedItemCountProperty()
								.subtract(1))
						// -1 Would mean that nothing is selected
						.or(this.imageTree.getSelectionModel().selectedIndexProperty()
								.isEqualTo(-1))
						// Make sure to negate because we want to hide the arrow when the above things are true
						.not());

		this.currentlySelectedImage.addListener((observable, oldValue, newValue) ->
		{
			// When we select a new image, reset the image viewport to center and zoomed out.
			this.resetImageView(null);
			// We also make sure to pull the image from online if it's a cloud based image
			if (newValue instanceof CloudImageEntry) ((CloudImageEntry) newValue).pullFromCloudIfNotPulled();
		});

		TableColumn<MetadataEntry, String> tagColumn = new TableColumn<>("Tag");
		tagColumn.setCellValueFactory(new PropertyValueFactory<>("tag"));
		tagColumn.setEditable(false);

		TableColumn<MetadataEntry, String> valueColumn = new TableColumn<>("Value");
		valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
		valueColumn.setCellFactory(x -> new EditCell<>(new DefaultStringConverter()));
		valueColumn.setEditable(true);
		valueColumn.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setValue(event.getNewValue()));

		this.metadataTableView.getColumns().add(tagColumn);
		this.metadataTableView.getColumns().add(valueColumn);

		this.metadataTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		this.metadataTableView.setEditable(true);
		this.metadataTableView.itemsProperty().bind(EasyBind.monadic(this.currentlySelectedImage)
				.map(ImageEntry::getImageMetadata)
				.map(observableList -> {
					FilteredList<MetadataEntry> filteredObservableList = new FilteredList<>(observableList);
					filteredObservableList.predicateProperty().bind(Bindings.createObjectBinding(() -> filteredItem -> StringUtils.containsIgnoreCase(filteredItem.getTag().getName(), this.txtMetadataSearch.getCharacters()), this.txtMetadataSearch.textProperty()));
					return filteredObservableList;
				})
		);

		this.currentlySelectedImage.addListener((observable, oldValue, newValue) -> {
			if (newValue != null)
			{
				System.out.println(SanimalData.getInstance().getNeonData().closestSiteTo(newValue.getSpecificMetadataField(StandardTag.GPS_LATITUDE), newValue.getSpecificMetadataField(StandardTag.GPS_LONGITUDE)));
			}
		});

		this.lvwSites.setCellFactory(x -> FXMLLoaderUtils.loadFXML("importView/SiteListEntry.fxml").getController());
		ObservableList<Site> sites = SanimalData.getInstance().getNeonData().getSites();
		SortedList<Site> sortedSites = new SortedList<>(sites);
		sortedSites.setComparator(Comparator.comparing(Site::getSiteName));
		this.lvwSites.setItems(sortedSites);

		// Initialize the fade transitions

		// First create a fade-in transition for the add a new species hover
		this.fadeAddPanelIn = new FadeTransition(Duration.millis(100), this.imageAddOverlay);
		this.fadeAddPanelIn.setFromValue(0.5);
		this.fadeAddPanelIn.setToValue(0);
		this.fadeAddPanelIn.setCycleCount(1);

		// First create a fade-out transition for the add a new species hover
		this.fadeAddPanelOut = new FadeTransition(Duration.millis(100), this.imageAddOverlay);
		this.fadeAddPanelOut.setFromValue(0);
		this.fadeAddPanelOut.setToValue(0.5);
		this.fadeAddPanelOut.setCycleCount(1);

		// Create a fade-in transition for the left and right arrow
		this.fadeLeftIn = new FadeTransition(Duration.millis(100), this.btnLeftArrow);
		this.fadeLeftIn.setFromValue(0);
		this.fadeLeftIn.setToValue(1);
		this.fadeLeftIn.setCycleCount(1);
		this.fadeRightIn = new FadeTransition(Duration.millis(100), this.btnRightArrow);
		this.fadeRightIn.setFromValue(0);
		this.fadeRightIn.setToValue(1);
		this.fadeRightIn.setCycleCount(1);

		// Create a fade-out transition for the left and right arrow
		this.fadeLeftOut = new FadeTransition(Duration.millis(100), this.btnLeftArrow);
		this.fadeLeftOut.setFromValue(1);
		this.fadeLeftOut.setToValue(0);
		this.fadeLeftOut.setCycleCount(1);
		this.fadeRightOut = new FadeTransition(Duration.millis(100), this.btnRightArrow);
		this.fadeRightOut.setFromValue(1);
		this.fadeRightOut.setToValue(0);
		this.fadeRightOut.setCycleCount(1);

		// Force play all fade ins to start
		this.fadeAddPanelIn.play();
		this.fadeLeftIn.play();
		this.fadeRightIn.play();
	}

	/**
	 * Fired when the import images button is pressed
	 *
	 * @param actionEvent consumed when the button is pressed
	 */
	public void importImages(ActionEvent actionEvent)
	{
		// Create a directory chooser to let the user choose where to get the images from
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Select Folder with Images");
		// Set the directory to be in documents
		directoryChooser.setInitialDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());
		// Show the dialog
		File file = directoryChooser.showDialog(this.imagePreview.getScene().getWindow());
		// If the file chosen is a file and a directory process it
		if (file != null && file.isDirectory())
		{
			this.btnImportImages.setDisable(true);
			Task<ImageDirectory> importTask = new ErrorTask<ImageDirectory>()
			{
				@Override
				protected ImageDirectory call()
				{
					final Long MAX_WORK = 6L;

					this.updateProgress(1, MAX_WORK);
					this.updateMessage("Loading directory...");

					// Grab the current list of species and locations and duplicate it
					List<Species> currentSpecies = new ArrayList<>(SanimalData.getInstance().getSpeciesList());
					List<Location> currentLocations = new ArrayList<>(SanimalData.getInstance().getLocationList());

					// Convert the file to a recursive image directory data structure
					ImageDirectory directory = DirectoryManager.loadDirectory(file, currentLocations, currentSpecies);

					this.updateProgress(2, MAX_WORK);
					this.updateMessage("Removing empty directories...");

					// Remove any directories that are empty and contain no images
					DirectoryManager.removeEmptyDirectories(directory);

					this.updateProgress(5, MAX_WORK);
					this.updateMessage("Adding images to the visual tree...");

					this.updateProgress(6, MAX_WORK);
					this.updateMessage("Finished!");

					return directory;
				}
			};

			importTask.setOnSucceeded(event ->
			{
				// Add the directory to the image tree
				SanimalData.getInstance().getImageTree().addChild(importTask.getValue());
				this.btnImportImages.setDisable(false);
			});

			SanimalData.getInstance().getSanimalExecutor().getQueuedExecutor().addTask(importTask);
		}
		// Consume the event
		actionEvent.consume();
	}

	/**
	 * if the delete images button is pressed
	 *
	 * @param actionEvent Button click is consumed
	 */
	public void deleteImages(ActionEvent actionEvent)
	{
		// Grab the selected item
		TreeItem<ImageContainer> item = this.imageTree.getSelectionModel().getSelectedItem();
		// Remove that item from the image tree
		SanimalData.getInstance().getImageTree().removeChildRecursive(item.getValue());
		// Make sure to clear the selection in the tree. This ensures that our left & right arrows will properly hide themselves if no more directories are present
		this.imageTree.getSelectionModel().clearSelection();
		// Consume the event
		actionEvent.consume();
	}

	/**
	 * Reset the image view if the reset button is clicked or a new image is selected
	 *
	 * @param actionEvent consumed if an event is given, otherwise ignored
	 */
	public void resetImageView(ActionEvent actionEvent)
	{
		// Reset the image preview viewport to its default state
		if (this.imagePreview.getImage() != null)
		{
			double imageWidth = this.imagePreview.getImage().getWidth();
			double imageHeight = this.imagePreview.getImage().getHeight();
			this.imagePreview.setViewport(new Rectangle2D(0, 0, imageWidth, imageHeight));
		}
		// Consume the event if possible
		if (actionEvent != null)
			actionEvent.consume();
	}

	/**
	 * If our mouse hovers over the image pane and we're dragging, we accept the transfer
	 *
	 * @param dragEvent The event that means we are dragging over the image pane
	 */
	public void imagePaneDragOver(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we started dragging at the species or location view and the dragboard has a string we play the fade animation and consume the event
		if ((dragboard.hasContent(SanimalDataFormats.LOCATION_NAME_FORMAT) && dragboard.hasContent(SanimalDataFormats.LOCATION_ID_FORMAT)) || (dragboard.hasContent(SanimalDataFormats.SPECIES_NAME_FORMAT) && dragboard.hasContent(SanimalDataFormats.SPECIES_SCIENTIFIC_NAME_FORMAT) && this.currentlySelectedImage.getValue() != null))
			dragEvent.acceptTransferModes(TransferMode.COPY);
		dragEvent.consume();
	}

	/**
	 * When the drag from the species or location list enters the image
	 *
	 * @param dragEvent The event that means we are dragging over the image pane
	 */
	public void imagePaneDragEntered(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we started dragging at the species or location view and the dragboard has a string we play the fade animation and consume the event
		if ((dragboard.hasContent(SanimalDataFormats.LOCATION_NAME_FORMAT) && dragboard.hasContent(SanimalDataFormats.LOCATION_ID_FORMAT)) || (dragboard.hasContent(SanimalDataFormats.SPECIES_NAME_FORMAT) && dragboard.hasContent(SanimalDataFormats.SPECIES_SCIENTIFIC_NAME_FORMAT) && this.currentlySelectedImage.getValue() != null))
			this.fadeAddPanelOut.play();
		dragEvent.consume();
	}

	/**
	 * When the drag from the species or location list exits the image
	 *
	 * @param dragEvent The event that means we are dragging away from the image pane
	 */
	public void imagePaneDragExited(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we started dragging at the species or location view and the dragboard has a string we play the fade animation and consume the event
		if ((dragboard.hasContent(SanimalDataFormats.LOCATION_NAME_FORMAT) && dragboard.hasContent(SanimalDataFormats.LOCATION_ID_FORMAT)) || (dragboard.hasContent(SanimalDataFormats.SPECIES_NAME_FORMAT) && dragboard.hasContent(SanimalDataFormats.SPECIES_SCIENTIFIC_NAME_FORMAT) && this.currentlySelectedImage.getValue() != null))
			this.fadeAddPanelIn.play();
		dragEvent.consume();
	}

	/**
	 * When we drop the species or location onto the image, we add that species or location to the list
	 *
	 * @param dragEvent The event that means we are dragging away from the image pane
	 */
	public void imagePaneDragDropped(DragEvent dragEvent)
	{
		// Create a flag that will be set to true if everything went well
		Boolean success = false;
		// Grab the dragboard
		Dragboard dragboard = dragEvent.getDragboard();

		// Set the success equal to the flag, and consume the event
		dragEvent.setDropCompleted(success);
		dragEvent.consume();
	}

	/**
	 * When the user moves their mouse over the image show the left & right arrows
	 *
	 * @param mouseEvent ignored
	 */
	public void imagePaneMouseEntered(MouseEvent mouseEvent)
	{
		this.fadeLeftIn.play();
		this.fadeRightIn.play();
	}

	/**
	 * When the user moves their mouse over the image hide the left & right arrows
	 *
	 * @param mouseEvent ignored
	 */
	public void imagePaneMouseExited(MouseEvent mouseEvent)
	{
		this.fadeLeftOut.play();
		this.fadeRightOut.play();
	}

	/**
	 * When we click the left arrow we want to advance the picture to the next untagged image
	 *
	 * @param actionEvent ignored
	 */
	public void onLeftArrowClicked(ActionEvent actionEvent)
	{
		this.imageTree.getSelectionModel().selectPrevious();
	}

	/**
	 * When we click the right arrow we want to advance the picture to the next untagged image
	 *
	 * @param actionEvent ignored
	 */
	public void onRightArrowClicked(ActionEvent actionEvent)
	{
		this.imageTree.getSelectionModel().selectNext();
	}

	public void resetMetadataSearch(ActionEvent actionEvent)
	{
		this.txtMetadataSearch.clear();
	}

	///
	/// Everything after this point allows the user to scroll the image view. The library used was found here:
	/// https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
	///

	// Found here: https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
	public void onImagePressed(MouseEvent mouseEvent)
	{
		mouseDown.set(imageViewToImage(this.imagePreview, new Point2D(mouseEvent.getX(), mouseEvent.getY())));
	}

	// Found here: https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
	public void onImageDragged(MouseEvent mouseEvent)
	{
		Point2D dragPoint = imageViewToImage(this.imagePreview, new Point2D(mouseEvent.getX(), mouseEvent.getY()));
		shift(this.imagePreview, dragPoint.subtract(mouseDown.get()));
		mouseDown.set(imageViewToImage(this.imagePreview, new Point2D(mouseEvent.getX(), mouseEvent.getY())));
	}

	// Found here: https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
	public void onImageClicked(MouseEvent mouseEvent)
	{
		if (mouseEvent.getClickCount() >= 2)
			this.resetImageView(null);
	}

	// Found here: https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
	public void onImageScroll(ScrollEvent scrollEvent)
	{
		double delta = -scrollEvent.getDeltaY();
		Rectangle2D viewport = this.imagePreview.getViewport();

		double scale = clamp(Math.pow(1.01, delta),

				// don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
				Math.min(10 / viewport.getWidth(), 10 / viewport.getHeight()),

				// don't scale so that we're bigger than image dimensions:
				Math.max(this.imagePreview.getImage().getWidth() / viewport.getWidth(), this.imagePreview.getImage().getHeight() / viewport.getHeight())

		);

		Point2D mouse = imageViewToImage(imagePreview, new Point2D(scrollEvent.getX(), scrollEvent.getY()));

		double newWidth = viewport.getWidth() * scale;
		double newHeight = viewport.getHeight() * scale;

		// To keep the visual point under the mouse from moving, we need
		// (x - newViewportMinX) / (x - currentViewportMinX) = scale
		// where x is the mouse X coordinate in the image

		// solving this for newViewportMinX gives

		// newViewportMinX = x - (x - currentViewportMinX) * scale

		// we then clamp this value so the image never scrolls out
		// of the imageview:

		double newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale,
				0, this.imagePreview.getImage().getWidth() - newWidth);
		double newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale,
				0, this.imagePreview.getImage().getHeight() - newHeight);

		imagePreview.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
	}

	// convert mouse coordinates in the imageView to coordinates in the actual image:
	// Found here: https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
	private Point2D imageViewToImage(ImageView imageView, Point2D imageViewCoordinates)
	{
		double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
		double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

		Rectangle2D viewport = imageView.getViewport();
		return new Point2D(
				viewport.getMinX() + xProportion * viewport.getWidth(),
				viewport.getMinY() + yProportion * viewport.getHeight());
	}

	// shift the viewport of the imageView by the specified delta, clamping so
	// the viewport does not move off the actual image:
	// Found here: https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
	private void shift(ImageView imageView, Point2D delta)
	{
		Rectangle2D viewport = imageView.getViewport();

		double width = imageView.getImage().getWidth();
		double height = imageView.getImage().getHeight();

		double maxX = width - viewport.getWidth();
		double maxY = height - viewport.getHeight();

		double minX = clamp(viewport.getMinX() - delta.getX(), 0, maxX);
		double minY = clamp(viewport.getMinY() - delta.getY(), 0, maxY);

		imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
	}

	// Found here: https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
	private double clamp(double value, double min, double max)
	{
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
}
