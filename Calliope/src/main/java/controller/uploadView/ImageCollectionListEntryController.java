package controller.uploadView;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.CalliopeData;
import model.cyverse.ImageCollection;
import model.cyverse.Permission;
import model.dataSources.IDataSource;
import model.image.DataDirectory;
import model.image.ImageEntry;
import model.util.FXMLLoaderUtils;
import org.controlsfx.control.action.Action;

import java.util.Objects;
import java.util.Optional;

import static model.constant.CalliopeDataFormats.IMAGE_DIRECTORY_FILE_FORMAT;

/**
 * Controller class for the image collection list
 */
public class ImageCollectionListEntryController extends ListCell<ImageCollection>
{
	///
	/// FXML Bound Fields Start
	///

	// The main background pane
	@FXML
	public HBox mainPane;

	// The labels for the collection pieces
	@FXML
	public Label lblCollectionName;
	@FXML
	public Label lblCollectionContactInfo;
	@FXML
	public Label lblCollectionOrganization;
	@FXML
	public Label lblCollectionDescription;

	// The button to access colleciton settings
	@FXML
	public Button btnSettings;

	// Images used to display user permissions
	@FXML
	public ImageView imgRead;
	@FXML
	public ImageView imgUpload;
	@FXML
	public ImageView imgOwner;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Called once the controller has been setup
	 */
	@FXML
	public void initialize()
	{
		Tooltip.install(this.imgRead, new Tooltip("You may see images uploaded to this collection."));
		Tooltip.install(this.imgUpload, new Tooltip("You may upload images to this collection."));
		Tooltip.install(this.imgOwner, new Tooltip("You are the owner of this collection."));
	}

	/**
	 * Update item is called when the list cell gets a new collection
	 *
	 * @param collection The new collection
	 * @param empty if the cell will be empty
	 */
	@Override
	protected void updateItem(ImageCollection collection, boolean empty)
	{
		// Update the cell internally
		super.updateItem(collection, empty);

		// Set the text of the cell to nothing
		this.setText(null);

		// If no collection was given and the cell was empty, clear the graphic
		if (empty && collection == null)
		{
			this.setGraphic(null);
		}
		else
		{
			// Set the name to the collection name
			this.lblCollectionName.setText(collection.getName());
			// Set the contact info to be the collection contact info
			this.lblCollectionContactInfo.setText(collection.getContactInfo());
			// Set the organization of the collection
			this.lblCollectionOrganization.setText(collection.getOrganization());
			// Set the description of the collection
			this.lblCollectionDescription.setText(collection.getDescription());
			// Hide the settings button if we are not the owner
			Permission forUser = collection.getPermissions().stream().filter(perm -> perm.getUsername().equals(CalliopeData.getInstance().getUsername())).findFirst().orElse(null);
			boolean isOwner = forUser != null && forUser.isOwner();
			boolean canUpload = forUser != null && forUser.canUpload();
			boolean canRead = forUser != null && forUser.canRead();
			// Hide the owner, upload and read icons if we do not have the respective permission
			this.btnSettings.setDisable(!isOwner);
			this.imgOwner.setVisible(isOwner);
			this.imgUpload.setVisible(canUpload);
			this.imgRead.setVisible(canRead);
			// Set the graphic to display
			this.setGraphic(mainPane);
		}
	}

	/**
	 * Called when the settings button is selected
	 *
	 * @param actionEvent consumed
	 */
	public void settingsClicked(ActionEvent actionEvent)
	{
		// Load the FXML file of the editor window
		FXMLLoader loader = FXMLLoaderUtils.loadFXML("uploadView/ImageCollectionSettings.fxml");
		// Grab the controller and set the species of that controller
		ImageCollectionSettingsController controller = loader.getController();
		controller.setCollectionToEdit(this.getItem());

		if (!CalliopeData.getInstance().getSettings().getDisablePopups())
		{
			// Create the stage that will have the Image Collection Editor
			Stage dialogStage = new Stage();
			// Set the title
			dialogStage.setTitle("Image Collection Editor");
			// Set the modality and initialize the owner to be this current window
			dialogStage.initModality(Modality.WINDOW_MODAL);
			dialogStage.initOwner(this.mainPane.getScene().getWindow());
			// Set the scene to the root of the FXML file
			Scene scene = new Scene(loader.getRoot());
			// Set the scene of the stage, and show it!
			dialogStage.setScene(scene);
			dialogStage.showAndWait();
		}
		else
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Popups must be enabled to edit a collection!");
		}
		if (actionEvent != null)
			actionEvent.consume();
	}

	/**
	 * If our mouse hovers over the image pane and we're dragging, we accept the transfer
	 *
	 * @param dragEvent The event that means we are dragging over the image pane
	 */
	public void cellDragOver(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we started dragging from a directory we accept the transfer
		if (dragboard.hasContent(IMAGE_DIRECTORY_FILE_FORMAT))
			dragEvent.acceptTransferModes(TransferMode.COPY);
		dragEvent.consume();
	}

	/**
	 * When the drag from the image directory enters the collection
	 *
	 * @param dragEvent The event that means we are dragging over the collection
	 */
	public void cellDragEntered(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we started dragging at the image directory and the dragboard has a string we update the CSS and consume the event
		if (dragboard.hasContent(IMAGE_DIRECTORY_FILE_FORMAT))
			if (!this.mainPane.getStyleClass().contains("draggedOver"))
				this.mainPane.getStyleClass().add("draggedOver");
		dragEvent.consume();
	}

	/**
	 * When the drag from the image directory exits the collection
	 *
	 * @param dragEvent The event that means we are dragging away from the collection
	 */
	public void cellDragExited(DragEvent dragEvent)
	{
		Dragboard dragboard = dragEvent.getDragboard();
		// If we started dragging at the image directory and the dragboard has a string we update the CSS and consume the event
		if (dragboard.hasContent(IMAGE_DIRECTORY_FILE_FORMAT))
			this.mainPane.getStyleClass().remove("draggedOver");
		dragEvent.consume();
	}

	/**
	 * When we drop the image directory onto the collection, we perform the upload
	 *
	 * @param dragEvent The event used to ensure the drag is valid
	 */
	public void cellDragDropped(DragEvent dragEvent)
	{
		// Grab the dragboard
		Dragboard dragboard = dragEvent.getDragboard();
		// If our dragboard has a string we have data which we need
		if (dragboard.hasContent(IMAGE_DIRECTORY_FILE_FORMAT))
		{
			Integer imageDirectoryHash = (int) dragboard.getContent(IMAGE_DIRECTORY_FILE_FORMAT);
			// Filter our list of images by directory that has the right file path
			Optional<DataDirectory> imageDirectoryOpt = CalliopeData.getInstance().getImageTree()
					.flattened()
					.filter(imageContainer -> imageContainer instanceof DataDirectory &&
							Objects.hash(imageContainer) == imageDirectoryHash)
					.map(imageContainer -> (DataDirectory) imageContainer)
					.findFirst();

			// If we found the correct image directory to upload to, prompt the user
			imageDirectoryOpt.ifPresent(imageDirectory ->
				// Ask the user if they want to upload these images
				CalliopeData.getInstance().getErrorDisplay().notify("Are you sure you want to upload/index these " + imageDirectory.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry).count() + " images to the collection " + this.getItem().getName() + "?",
					// If "Yes" is pressed, perform the action
					new Action("Yes", actionEvent ->
					{
						// Grab the source of the data
						IDataSource dataSource = imageDirectory.getDataSource();
						// Make an index task for this directory
						Task<Void> indexTask = dataSource.makeIndexTask(ImageCollectionListEntryController.this.getItem(), imageDirectory);
						// Execute this index task
						if (indexTask != null)
							CalliopeData.getInstance().getExecutor().getImmediateExecutor().addTask(indexTask, true);
						dragEvent.setDropCompleted(true);
					})));
		}
		dragEvent.consume();
	}
}
