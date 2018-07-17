package controller;

import controller.importView.NeonSiteDetectorController;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import jfxtras.scene.control.LocalDateTimeTextField;
import library.ImageViewPane;
import library.TreeViewAutomatic;
import model.CalliopeData;
import model.constant.CalliopeDataFormats;
import model.dataSources.IDataSource;
import model.image.*;
import model.neon.BoundedSite;
import model.neon.jsonPOJOs.Site;
import model.threading.ErrorService;
import model.threading.ErrorTask;
import model.util.FXMLLoaderUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.StatusBar;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;
import org.controlsfx.property.editor.PropertyEditor;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller class for the main import window
 */
public class CalliopeImportController
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

	// The list view containing all locations
	@FXML
	public ListView<BoundedSite> siteListView;

	// The button to reset the image effects
	@FXML
	public Button btnResetImage;

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

	// The progress indicator showing that an image is being loaded
	@FXML
	public ProgressIndicator pidImageLoading;

	// The main pane holding everything
	@FXML
	public SplitPane mainPane;

	// Top right label containing location name
	@FXML
	public Label lblSite;
	// Top right hbox containing location info
	@FXML
	public HBox hbxLocation;

	// The button used to rebuild the NEON data index
	@FXML
	public Button btnRefreshNEONSites;

	// The text field used to search the list of sites
	@FXML
	public TextField txtSiteSearch;

	// A grid pane of editable metadata attributes
	@FXML
	public GridPane gpnEditableMetadata;
	// The bottom field representing the date the image was taken
	@FXML
	public LocalDateTimeTextField txtDateTaken;
	// The bottom field representing the latitude of the image
	@FXML
	public TextField txtLatitude;
	// The bottom field representing the longitude of the image
	@FXML
	public TextField txtLongitude;
	// The bottom field representing the elevation of the image
	@FXML
	public TextField txtElevation;

	// The bottom field representing the drone's brand
	@FXML
	public TextField txtDroneBrand;
	// The bottom field representing the drone's model
	@FXML
	public TextField txtCameraModel;
	// The bottom fields representing the X, Y, and Z speed of the drone
	@FXML
	public TextField txtXSpeed;
	@FXML
	public TextField txtYSpeed;
	@FXML
	public TextField txtZSpeed;
	// The bottom field representing the X, Y, and Z rotation of the drone (yaw, roll, pitch)
	@FXML
	public TextField txtXRotation;
	@FXML
	public TextField txtYRotation;
	@FXML
	public TextField txtZRotation;

	// The left tab pane which contains metadata or neon site list
	@FXML
	public TabPane leftTabPane;

	// The propertysheet used by the left tab pane to show metadata
	@FXML
	public PropertySheet pstMetadata;

	// A list of possible import options when adding data to the program
	@FXML
	public ComboBox<IDataSource> cbxImport;


	///
	/// FXML bound fields end
	///

	// Fields to hold the currently selected image entry and image directory
	private ObjectProperty<ImageEntry> currentlySelectedImage = new SimpleObjectProperty<>(null);
	private ObjectProperty<ImageDirectory> currentlySelectedDirectory = new SimpleObjectProperty<>(null);
	// Use fade transitions to fade the species list in and out
	private FadeTransition fadeLocationIn;
	private FadeTransition fadeLocationOut;
	private FadeTransition fadeAddPanelIn;
	private FadeTransition fadeAddPanelOut;
	private FadeTransition fadeLeftIn;
	private FadeTransition fadeLeftOut;
	private FadeTransition fadeRightIn;
	private FadeTransition fadeRightOut;
	// A property used to process the image scrolling
	private ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

	// The current image being previewed
	private ObjectProperty<Image> speciesPreviewImage = new SimpleObjectProperty<>(null);

	// A list of Property<Object> that is used to store weak listeners to avoid early garbage collection. This concept is strange and difficult to
	// understand, here's some articles on it:
	// https://stackoverflow.com/questions/23785816/javafx-beans-binding-suddenly-stops-working
	// https://stackoverflow.com/questions/14558266/clean-javafx-property-listeners-and-bindings-memory-leaks
	// https://stackoverflow.com/questions/26312651/bidirectional-javafx-binding-is-destroyed-by-unrelated-code
	private List<Property<?>> cache = new ArrayList<>();

	// The stage used to hold the neon site detector content
	private Stage neonSiteDetectorStage;
	// The controller which controls the neon site detector stage
	private NeonSiteDetectorController neonSiteDetectorController;

	/**
	 * Initialize the Calliope import view and data bindings
	 */
	@FXML
	public void initialize()
	{
		// Setup the tab pane on the left to have uniformly sized tabs
		leftTabPane.tabMinWidthProperty().bind(leftTabPane.widthProperty().divide(leftTabPane.getTabs().size()).subtract(30));

		// Then we setup the site list

		// Grab the global site list
		SortedList<BoundedSite> sites = new SortedList<>(CalliopeData.getInstance().getSiteList());
		// Set the comparator to be the name of the site
		sites.setComparator(Comparator.comparing(boundedSite -> boundedSite.getSite().getSiteName()));
		// Create a filtered list of sites
		FilteredList<BoundedSite> filteredSites = new FilteredList<>(sites);
		// Set the filter to update whenever the site search text changes
		filteredSites.predicateProperty().bind(Bindings.createObjectBinding(() -> (siteToFilter ->
				// Allow any site with a name or code search text
				(StringUtils.containsIgnoreCase(siteToFilter.getSite().getSiteName(), this.txtSiteSearch.getCharacters()) ||
				 StringUtils.containsIgnoreCase(siteToFilter.getSite().getSiteCode(), this.txtSiteSearch.getCharacters()))), this.txtSiteSearch.textProperty()));
		// Set the items of the site list view to the newly sorted list
		this.siteListView.setItems(filteredSites);
		// Set the cell factory to be our custom location list cell
		this.siteListView.setCellFactory(x -> FXMLLoaderUtils.loadFXML("importView/SiteListEntry.fxml").getController());

		// Initialize root of the right side directory/image tree and make the root invisible
		// This is because a treeview must have ONE root.

		// Create a fake invisible root node whos children
		final TreeItem<ImageContainer> ROOT = new TreeItem<>(CalliopeData.getInstance().getImageTree());
		// Hide the fake invisible root
		this.imageTree.setShowRoot(false);
		// Set the fake invisible root
		this.imageTree.setRoot(ROOT);
		// Set the items of the tree to be the children of the fake invisible root
		this.imageTree.setItems(CalliopeData.getInstance().getImageTree().getChildren());
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

		// When a new image is selected... we perform a bunch of actions below
		MonadicBinding<ImageContainer> selectedImage = EasyBind.monadic(this.imageTree.getSelectionModel().selectedItemProperty()).map(TreeItem::getValue);
		// Clear the preview pane if there is a preview'd image
		selectedImage.addListener((observable, oldValue, newValue) -> this.speciesPreviewImage.setValue(null));
		// Update the currently selected image and directory
		currentlySelectedImage.bind(selectedImage.map(imageContainer -> (imageContainer instanceof ImageEntry) ? (ImageEntry) imageContainer : null));
		currentlySelectedDirectory.bind(selectedImage.map(imageContainer -> (imageContainer instanceof ImageDirectory) ? (ImageDirectory) imageContainer : null));

		// Hide the delete button when nothing is selected
		this.btnDelete.disableProperty().bind(this.imageTree.getSelectionModel().selectedIndexProperty().isEqualTo(-1));

		// Create bindings in the GUI

		// A converter used to convert from strings to numbers and back
		StringConverter<Number> numStrconverter = new StringConverter<Number>()
		{
			/**
			 * Function that takes a number as input and returns it as a string
			 *
			 * @param number The number to convert
			 * @return The string representation of the number
			 */
			@Override
			public String toString(Number number)
			{
				if (number != null)
					return number.toString();
				else
					return "";
			}

			/**
			 * Function takes a string as input and returns it as a number or 0 if it is invalid
			 *
			 * @param string The number as a string to convert
			 * @return The number string as a number
			 */
			@Override
			public Number fromString(String string)
			{
				if (string != null)
					return NumberUtils.toDouble(string, 0);
				else
					return 0;
			}
		};

		// Hide the reset button when we do not have an image selected
		this.btnResetImage.disableProperty().bind(currentlySelectedImage.isNull());

		///
		/// The next properties are bound image fields. If no image is selected the text fields are disabled. We use bi-directional bindings so that
		/// if we change the text field the model updates, and if the model is updated internally the text fields update.
		///

		MonadicBinding<Boolean> metadataEnabled = EasyBind.monadic(this.currentlySelectedImage).selectProperty(ImageEntry::metadataEditableProperty).map(x -> !x).orElse(true);

		this.txtDateTaken.disableProperty().bind(metadataEnabled);
		this.txtDateTaken.localDateTimeProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::dateTakenProperty)));
		this.txtLatitude.disableProperty().bind(metadataEnabled);
		this.txtLatitude.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::locationTakenProperty).selectProperty(Position::latitudeProperty)), numStrconverter);
		this.txtLongitude.disableProperty().bind(metadataEnabled);
		this.txtLongitude.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::locationTakenProperty).selectProperty(Position::longitudeProperty)), numStrconverter);
		this.txtElevation.disableProperty().bind(metadataEnabled);
		this.txtElevation.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::locationTakenProperty).selectProperty(Position::elevationProperty)), numStrconverter);
		this.txtDroneBrand.disableProperty().bind(metadataEnabled);
		this.txtDroneBrand.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::droneMakerProperty)));
		this.txtCameraModel.disableProperty().bind(metadataEnabled);
		this.txtCameraModel.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::cameraModelProperty)));
		this.txtXSpeed.disableProperty().bind(metadataEnabled);
		this.txtXSpeed.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::speedProperty).selectProperty(Vector3::xProperty)), numStrconverter);
		this.txtYSpeed.disableProperty().bind(metadataEnabled);
		this.txtYSpeed.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::speedProperty).selectProperty(Vector3::yProperty)), numStrconverter);
		this.txtZSpeed.disableProperty().bind(metadataEnabled);
		this.txtZSpeed.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::speedProperty).selectProperty(Vector3::zProperty)), numStrconverter);
		this.txtXRotation.disableProperty().bind(metadataEnabled);
		this.txtXRotation.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::rotationProperty).selectProperty(Vector3::xProperty)), numStrconverter);
		this.txtYRotation.disableProperty().bind(metadataEnabled);
		this.txtYRotation.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::rotationProperty).selectProperty(Vector3::yProperty)), numStrconverter);
		this.txtZRotation.disableProperty().bind(metadataEnabled);
		this.txtZRotation.textProperty().bindBidirectional(cache(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::rotationProperty).selectProperty(Vector3::zProperty)), numStrconverter);
		// Service used to retrieve the currently selected image pixel data
		ErrorService<Image> imageRetrievalService = new ErrorService<Image>()
		{
			@Override
			protected Task<Image> createTask()
			{
				ImageEntry value = currentlySelectedImage.getValue();
				return new ErrorTask<Image>()
				{
					@Override
					protected Image call()
					{
						// Passing in null returns null
						if (value == null)
							return null;
						// Perform the hard computation
						return value.buildDisplayableImage();
					}
				};
			}
		};
		// When the service finishes we update the displayed image and hide the loading gif
		imageRetrievalService.setOnSucceeded(event ->
		{
			this.imagePreview.setImage(imageRetrievalService.getValue());
			this.pidImageLoading.setVisible(false);
		});
		// When we select a new image we retrieve the pixel data to display on screen
		this.currentlySelectedImage.addListener((observable, oldValue, newValue) ->
		{
			this.pidImageLoading.setVisible(true);
			imageRetrievalService.restart();
		});
		// When we click a new image reset the image view
		this.imagePreview.imageProperty().addListener((observable, oldValue, newValue) -> this.resetImageView(null));
		// Bind the site name to the selected image's location
		this.lblSite.textProperty().bind(EasyBind.monadic(currentlySelectedImage).selectProperty(ImageEntry::siteTakenProperty).selectProperty(BoundedSite::siteProperty).map(Site::getSiteName));
		// Hide the location panel when no location is selected
		this.hbxLocation.visibleProperty().bind(currentlySelectedImage.isNotNull().or(currentlySelectedDirectory.isNotNull()));
		// Hide the progress bar when no tasks remain
		this.sbrTaskProgress.visibleProperty().bind(CalliopeData.getInstance().getExecutor().getQueuedExecutor().taskRunningProperty());
		// Bind the progress bar's text property to tasks remaining
		this.sbrTaskProgress.textProperty().bind(CalliopeData.getInstance().getExecutor().getQueuedExecutor().messageProperty());
		// Bind the progress bar's progress property to the current task's progress
		this.sbrTaskProgress.progressProperty().bind(CalliopeData.getInstance().getExecutor().getQueuedExecutor().progressProperty());
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

		// When we select a new image, reset the image viewport to center and zoomed out.
		this.currentlySelectedImage.addListener((observable, oldValue, newValue) -> this.resetImageView(null));

		// Setup the neon site detector stage

		// Load the FXML file of the editor window
		FXMLLoader neonSiteDetectorLoader = FXMLLoaderUtils.loadFXML("importView/NeonSiteDetector.fxml");
		// Grab the controller
		this.neonSiteDetectorController = neonSiteDetectorLoader.getController();

		// Create the stage that will have the neon site detector
		this.neonSiteDetectorStage = new Stage();
		// Set the title
		this.neonSiteDetectorStage.setTitle("Neon Site Detector");
		// Set the modality and initialize the owner to be this current window
		this.neonSiteDetectorStage.initModality(Modality.WINDOW_MODAL);
		// Make sure the window is the right size and can't be resized
		this.neonSiteDetectorStage.setResizable(false);
		// Set the scene to the root of the FXML file
		Scene neonSiteDetectorScene = new Scene(neonSiteDetectorLoader.getRoot());
		// Set the scene of the stage, and show it!
		this.neonSiteDetectorStage.setScene(neonSiteDetectorScene);
		// When we close the stage don't close it, just hide it
		this.neonSiteDetectorStage.setOnCloseRequest(event ->
		{
			this.neonSiteDetectorStage.hide();
			event.consume();
		});

		// Setup the metadata property sheet

		// When we click a new image then load new metadata
		this.currentlySelectedImage.addListener((observable, oldValue, newValue) -> { if (newValue != null) this.pstMetadata.getItems().setAll(newValue.getRawMetadata()); });
		// Create a default factory
		DefaultPropertyEditorFactory defaultFactory = new DefaultPropertyEditorFactory();
		// Ensure that our editors are non-editable since metadata isn't editable
		this.pstMetadata.setPropertyEditorFactory(item ->
		{
			// Call the default factory
			PropertyEditor<?> toReturn = defaultFactory.call(item);
			// If it returns a text field, make sure we can't edit it
			if (toReturn.getEditor() instanceof TextField)
				((TextField) toReturn.getEditor()).setEditable(false);
			return toReturn;
		});

		// Setup the import combo-box

		// Set the items to be the enum possible values
		this.cbxImport.setItems(CalliopeData.getInstance().getDataSources());
		// Set our cell factory to be our custom cell
		this.cbxImport.setCellFactory(x -> FXMLLoaderUtils.loadFXML("importView/ImportableFormatEntry.fxml").getController());
		// Make sure that new cells use the 'getName' function to get the combo-box value instead of 'toString()'
		this.cbxImport.setConverter(new StringConverter<IDataSource>()
		{
			@Override
			public String toString(IDataSource dataSource)
			{
				return dataSource.getName();
			}
			@Override
			public IDataSource fromString(String dataSourceName)
			{
				return CalliopeData.getInstance().getDataSources().stream().filter(dataSource -> dataSource.getName().equals(dataSourceName)).findFirst().orElse(null);
			}
		});
		// When we select a new item in the list cell, execute the task and don't forget to disable the button while it's running
		EasyBind.subscribe(this.cbxImport.getSelectionModel().selectedItemProperty(), newValue ->
		{
			// If we got a valid new value
			if (newValue != null)
			{
				// Create the import task from the enum
				Task<ImageDirectory> importTask = newValue.makeImportTask(this.imageTree.getScene().getWindow());
				// Make sure the task is not null
				if (importTask != null)
				{
					// Hide the import button for now
					this.cbxImport.setDisable(true);
					// Grab the original on succeeded handler
					EventHandler<WorkerStateEvent> onSucceeded = importTask.getOnSucceeded();
					// Update our on succeeded handler to re-enable the import button
					importTask.setOnSucceeded(event ->
					{
						onSucceeded.handle(event);
						ImageDirectory value = importTask.getValue();

						// Also clear the selection since we're using this combo-box as more of an item list than anything
						// Because we're in a listener we can't actually modify the combobox in here, so use Platform.runLater to put it into a queue
						Platform.runLater(() -> this.cbxImport.getSelectionModel().clearSelection());
						this.cbxImport.setDisable(false);
					});
					// Execute the task
					CalliopeData.getInstance().getExecutor().getQueuedExecutor().addTask(importTask);
				}
				else
				{
					// Because we're in a listener we can't actually modify the combobox in here, so use Platform.runLater to put it into a queue
					Platform.runLater(() -> this.cbxImport.getSelectionModel().clearSelection());
				}
			}
		});

		// Initialize the fade transitions

		// First create a fade-in transition for the location
		this.fadeLocationIn = new FadeTransition(Duration.millis(100), this.hbxLocation);
		this.fadeLocationIn.setFromValue(1);
		this.fadeLocationIn.setToValue(0.3);
		this.fadeLocationIn.setCycleCount(1);

		// Then create a fade-out transition for the location
		this.fadeLocationOut = new FadeTransition(Duration.millis(100), this.hbxLocation);
		this.fadeLocationOut.setFromValue(0.3);
		this.fadeLocationOut.setToValue(1);
		this.fadeLocationOut.setCycleCount(1);

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
		this.fadeLocationIn.play();
		this.fadeAddPanelIn.play();
		this.fadeLeftIn.play();
		this.fadeRightIn.play();
	}

	/**
	 * This is purely used so that the action listeners are not garbage collected early
	 *
	 * @param reference The reference to cache
	 * @param <T> The type of the property, can be anything
	 * @return Returns the property passed in purely for convenience
	 */
	private <T> Property<T> cache(Property<T> reference)
	{
		this.cache.add(reference);
		return reference;
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
		CalliopeData.getInstance().getImageTree().removeChildRecursive(item.getValue());
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
	 * Allow the site list to be drag & dropable onto the image view
	 *
	 * @param mouseEvent consumed if a site is selected
	 */
	public void locationListDrag(MouseEvent mouseEvent)
	{
		// Grab the selected site, make sure it's not null
		BoundedSite selected = this.siteListView.getSelectionModel().getSelectedItem();
		if (selected != null)
		{
			// Create a dragboard and begin the drag and drop
			Dragboard dragboard = this.siteListView.startDragAndDrop(TransferMode.ANY);

			// Create a clipboard and put the location unique ID into that clipboard
			ClipboardContent content = new ClipboardContent();
			content.put(CalliopeDataFormats.SITE_CODE_FORMAT, selected.getSite().getSiteCode());
			// Set the dragboard's context, and then consume the event
			dragboard.setContent(content);

			mouseEvent.consume();
		}
	}

	/**
	 * If our mouse hovers over the image pane and we're dragging, we accept the transfer
	 *
	 * @param dragEvent The event that means we are dragging over the image pane
	 */
	public void imagePaneDragOver(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we started dragging at the site view and the dragboard has a string we play the fade animation and consume the event
		if (dragboard.hasContent(CalliopeDataFormats.SITE_CODE_FORMAT) && (this.currentlySelectedImage.getValue() != null || this.currentlySelectedDirectory.getValue() != null))
			dragEvent.acceptTransferModes(TransferMode.COPY);
		dragEvent.consume();
	}

	/**
	 * When the drag from the site list enters the image
	 *
	 * @param dragEvent The event that means we are dragging over the image pane
	 */
	public void imagePaneDragEntered(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we started dragging at the site view and the dragboard has a string we play the fade animation and consume the event
		if (dragboard.hasContent(CalliopeDataFormats.SITE_CODE_FORMAT) && (this.currentlySelectedImage.getValue() != null || this.currentlySelectedDirectory.getValue() != null))
			this.fadeAddPanelOut.play();
		dragEvent.consume();
	}

	/**
	 * When the drag from the site list exits the image
	 *
	 * @param dragEvent The event that means we are dragging away from the image pane
	 */
	public void imagePaneDragExited(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we started dragging at the site view and the dragboard has a string we play the fade animation and consume the event
		if (dragboard.hasContent(CalliopeDataFormats.SITE_CODE_FORMAT) && (this.currentlySelectedImage.getValue() != null || this.currentlySelectedDirectory.getValue() != null))
			this.fadeAddPanelIn.play();
		dragEvent.consume();
	}

	/**
	 * When we drop the site onto the image, we add that species or location to the list
	 *
	 * @param dragEvent The event that means we are dragging away from the image pane
	 */
	public void imagePaneDragDropped(DragEvent dragEvent)
	{
		// Create a flag that will be set to true if everything went well
		Boolean success = false;
		// Grab the dragboard
		Dragboard dragboard = dragEvent.getDragboard();
		// If our dragboard has a string we have data which we need
		if (dragboard.hasContent(CalliopeDataFormats.SITE_CODE_FORMAT))
		{
			String siteCode = (String) dragboard.getContent(CalliopeDataFormats.SITE_CODE_FORMAT);
			// Grab the site with the given ID
			Optional<BoundedSite> toAdd = CalliopeData.getInstance().getSiteList().stream().filter(boundedSite -> boundedSite.getSite().getSiteCode().equals(siteCode)).findFirst();
			// Add the site to the image
			if (toAdd.isPresent())
				// Check if we have a selected image or directory to update!
				if (currentlySelectedImage.getValue() != null)
				{
					currentlySelectedImage.getValue().setSiteTaken(toAdd.get());
					// We request focus after a drag and drop so that arrow keys will continue to move the selected image down or up
					this.imageTree.requestFocus();
					success = true;
				}
				else if (currentlySelectedDirectory.getValue() != null)
				{
					currentlySelectedDirectory.getValue().setSiteTaken(toAdd.get());
					// We request focus after a drag and drop so that arrow keys will continue to move the selected image down or up
					this.imageTree.requestFocus();
					success = true;
				}
		}
		// Set the success equal to the flag, and consume the event
		dragEvent.setDropCompleted(success);
		dragEvent.consume();
	}

	/**
	 * Called to reload the current NEON site cache on the ES index
	 *
	 * @param actionEvent consumed
	 */
	public void refreshNEONSites(ActionEvent actionEvent)
	{
		// Disable the button during processing
		this.btnRefreshNEONSites.setDisable(true);

		// The refresh task just calls our ES manager
		ErrorTask<Void> refreshTask = new ErrorTask<Void>()
		{
			@Override
			protected Void call()
			{
				this.updateMessage("Updating NEON site index from latest data...");
				CalliopeData.getInstance().getEsConnectionManager().refreshNeonSiteCache();
				return null;
			}
		};

		// Enable the button after processing
		refreshTask.setOnSucceeded(event -> this.btnRefreshNEONSites.setDisable(false));

		// Add the task to be executed
		CalliopeData.getInstance().getExecutor().getQueuedExecutor().addTask(refreshTask);

		// Consume the event
		actionEvent.consume();
	}

	/**
	 * Reset the site search field when we press the button
	 *
	 * @param actionEvent consumed
	 */
	public void resetSiteSearch(ActionEvent actionEvent)
	{
		this.txtSiteSearch.clear();
		actionEvent.consume();
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
	 * When we move our mouse over the location we play a fade animation
	 *
	 * @param mouseEvent ignored
	 */
	public void onMouseEnteredLocation(MouseEvent mouseEvent)
	{
		fadeLocationOut.play();
	}

	/**
	 * When we move our mouse away from the location we play a fade animation
	 *
	 * @param mouseEvent ignored
	 */
	public void onMouseExitedLocation(MouseEvent mouseEvent)
	{
		fadeLocationIn.play();
	}

	/**
	 * When we click the location icon, either remove the location off of the image or pull the location if not present
	 *
	 * @param mouseEvent consumed
	 */
	public void mouseClickedLocation(MouseEvent mouseEvent)
	{
		List<ImageEntry> images = null;

		// If we have an image selected then we wrap that image in a list for processing
		if (this.currentlySelectedImage.getValue() != null)
			images = Collections.singletonList(this.currentlySelectedImage.getValue());
		// If an image is not selected, test if a directory is selected. If so grab the list of images in the directory
		else if (this.currentlySelectedDirectory.getValue() != null)
			images = this.currentlySelectedDirectory.getValue().flattened().filter(imageContainer -> imageContainer instanceof ImageEntry).map(imageContainer -> (ImageEntry) imageContainer).filter(imageEntry -> imageEntry.getLocationTaken() != null).collect(Collectors.toList());

		// If we got any images at all, process them
		if (images != null && !images.isEmpty())
		{
			// Pull the sub-images in the directory
			this.neonSiteDetectorController.updateItems(images);
			// Make sure that this stage belongs to the main stage
			if (this.neonSiteDetectorStage.getOwner() == null)
				this.neonSiteDetectorStage.initOwner(this.imageTree.getScene().getWindow());
			this.neonSiteDetectorStage.showAndWait();
		}

		mouseEvent.consume();
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
