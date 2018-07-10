package controller;

import controller.uploadView.ImageCollectionListEntryController;
import controller.uploadView.ImageUploadDownloadListEntryController;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import library.TreeViewAutomatic;
import model.CalliopeData;
import model.cyverse.ImageCollection;
import model.cyverse.Permission;
import model.image.*;
import model.threading.ErrorTask;
import model.util.FXMLLoaderUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.TaskProgressView;
import org.controlsfx.control.action.Action;
import org.fxmisc.easybind.EasyBind;

import java.net.URL;
import java.util.Comparator;
import java.util.ResourceBundle;

/**
 * Controller class for the upload page
 */
public class CalliopeUploadController implements Initializable
{
	///
	/// FXML Bound Fields Start
	///

	// The list view of collections
	@FXML
	public ListView<ImageCollection> collectionListView;

	// The new and delete collection buttons
	@FXML
	public Button btnNewCollection;
	@FXML
	public Button btnDeleteCollection;

	// The primary split pane
	@FXML
	public SplitPane spnMain;

	// Upload entry on the cloud used to download...
	@FXML
	public ListView<UploadedEntry> uploadListDownloadListView;

	@FXML
	public Button btnRefreshUploads;

	// The download box pane
	@FXML
	public VBox vbxDownloadList;

	@FXML
	public MaskerPane mpnDownloadUploads;

	// Pane containing savable images
	@FXML
	public SplitPane spnUploadSave;

	// The tree view of uploadable images
	@FXML
	public TreeViewAutomatic<ImageContainer> imageTree;

	// A list of upload tasks currently running
	@FXML
	public TaskProgressView<Task<?>> tpvUploads;

	// Search field for uploads
	@FXML
	public TextField txtUploadSearch;
	// Resets the search
	@FXML
	public Button btnResetSearch;

	@FXML
	public Button btnIndexExisting;

	///
	/// FXML Bound Fields End
	///

	// Constant strings used to display status
	private static final String STATUS_LOADING = "Loading collection uploads...";
	private static final String STATUS_DOWNLOADING = "Downloading collection uploads to edit...";


	// The currently selected image collection
	private ObjectProperty<ImageCollection> selectedCollection = new SimpleObjectProperty<>();

	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		// First setup the collection list

		// Grab the global collection list
		SortedList<ImageCollection> collections = new SortedList<>(CalliopeData.getInstance().getCollectionList());
		// Set the comparator to be the name of the image collection
		collections.setComparator(Comparator.comparing(ImageCollection::getName));
		// Set the list of items to be the collections
		this.collectionListView.setItems(CalliopeData.getInstance().getCollectionList());
		// Set the cell factory to be our custom cell factory
		this.collectionListView.setCellFactory(x -> {
			ImageCollectionListEntryController controller = FXMLLoaderUtils.loadFXML("uploadView/ImageCollectionListEntry.fxml").getController();
			// When we double click the collection list view items, we want to edit the collection settings
			controller.setOnMouseClicked(event -> {
				if (event.getClickCount() >= 2 && controller.getItem() != null && !controller.btnSettings.isDisabled())
					controller.settingsClicked(null);
			});
			return controller;
		});
		// When we select a new element, set the property
		this.collectionListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> this.selectedCollection.setValue(newValue));

		// If no collection is selected, disable the text fields and buttons
		BooleanBinding nothingSelected = selectedCollection.isNull();

		// Disable this button when we are not the owner of the collection
		this.btnDeleteCollection.disableProperty().bind(EasyBind.monadic(this.selectedCollection).map(collection ->
		{
			String ownerUsername = collection.getOwner();
			return ownerUsername == null || !ownerUsername.equals(CalliopeData.getInstance().getUsername());
		}).orElse(nothingSelected));

		this.btnRefreshUploads.disableProperty().bind(nothingSelected);
		this.btnIndexExisting.disableProperty().bind(nothingSelected);

		// Initialize root of the right side directory/image tree and make the root invisible
		// This is because a treeview must have ONE root.

		// Create a fake invisible root node whos children
		final TreeItem<ImageContainer> ROOT = new TreeItem<>(CalliopeData.getInstance().getImageTree());
		// Hide the fake invisible root
		this.imageTree.setShowRoot(false);
		// Set the fake invisible root
		this.imageTree.setRoot(ROOT);
		// Set the items of the tree to be the children of the fake invisible root
		this.imageTree.setItems(CalliopeData.getInstance().getImageTree().getChildren().filtered(imageContainer -> !(imageContainer instanceof ImageEntry)));
		// Setup the image tree cells so that when they get drag & dropped the species & locations can be tagged
		this.imageTree.setCellFactory(x -> FXMLLoaderUtils.loadFXML("uploadView/UploadTreeCell.fxml").getController());

		// Custom cell factory used to show upload downloads
		this.uploadListDownloadListView.setCellFactory(list -> FXMLLoaderUtils.loadFXML("uploadView/ImageUploadDownloadListEntry.fxml").<ImageUploadDownloadListEntryController>getController());
		// Bind the upload download to the current selection's uploads, Sort the uploads by date taken and filter them by the query
		this.uploadListDownloadListView.itemsProperty().bind(EasyBind.monadic(this.selectedCollection).map(imageCollection -> {
			ObservableList<UploadedEntry> uploads = imageCollection.getUploads();
			SortedList<UploadedEntry> sortedUploads = uploads.sorted((entry1, entry2) -> entry2.getUploadDate().compareTo(entry1.getUploadDate()));
			FilteredList<UploadedEntry> filteredSortedUploads = sortedUploads.filtered(x -> true);
			// Set the filter to update whenever the upload search text changes
			filteredSortedUploads.predicateProperty().bind(Bindings.createObjectBinding(() -> (cloudUploadEntry ->
					// Allow any cloud upload entry with a username cloud upload entry search text
					StringUtils.containsIgnoreCase(cloudUploadEntry.getUploadUser(), this.txtUploadSearch.getCharacters()) ||
							StringUtils.containsIgnoreCase(CalliopeData.getInstance().getSettings().formatDateTime(cloudUploadEntry.getUploadDate(), " "), this.txtUploadSearch.getCharacters()) ||
							StringUtils.containsIgnoreCase(cloudUploadEntry.getImageCount().toString(), this.txtUploadSearch.getCharacters())), this.txtUploadSearch.textProperty()));
			return filteredSortedUploads;
		}));

		// Hide the maskerpane since we're not retrieving downloads
		this.mpnDownloadUploads.setVisible(false);
		// When we select a new collection download the list of uploads to display
		this.selectedCollection.addListener((observable, oldValue, newValue) ->
		{
			if (newValue != null)
			{
				// Test to see if the collection needs to download its list of uploads first
				if (!newValue.uploadsWereSynced())
				{
					newValue.setUploadsWereSynced(true);
					this.syncUploadsForCollection(newValue);
				}
			}
		});

		ObservableList<Task<?>> activeTasks = CalliopeData.getInstance().getExecutor().getImmediateExecutor().getActiveTasks();

		// Bind the tasks
		EasyBind.listBind(this.tpvUploads.getTasks(), activeTasks);

		// This removes the "cancel" button from the task
		activeTasks.addListener(new ListChangeListener<Task<?>>()
		{
			// When the first task gets added we perform some initialization
			@Override
			public void onChanged(Change<? extends Task<?>> c)
			{
				// Grab the list view from the library control (which we dont have access to)
				Node listView = tpvUploads.lookup(".list-view");
				if (listView instanceof ListView)
					// Update the cell factory to use our custom factory
					((ListView<Task<?>>) listView).setCellFactory(param -> new TaskCell<>());

				// Initialization is done, so
				activeTasks.removeListener(this);
			}
		});
	}

	/**
	 * Method called to sync a list of uploads to a collection
	 *
	 * @param collection The image collection to download uploads for
	 */
	private void syncUploadsForCollection(ImageCollection collection)
	{
		// Disable the download list and show the loading label and circle
		this.mpnDownloadUploads.setVisible(true);
		this.mpnDownloadUploads.setText(STATUS_LOADING);
		this.vbxDownloadList.setDisable(true);

		// Create a task to pull a collection's uploads
		ErrorTask<Void> collectionUploadDownloader = new ErrorTask<Void>()
		{
			@Override
			protected Void call()
			{
				this.updateMessage("Downloading list of uploads to collection: " + collection.getName());
				DoubleProperty progress = new SimpleDoubleProperty(0.0);
				progress.addListener((observable, oldValue, newValue) -> this.updateProgress(progress.getValue(), 1.0));
				CalliopeData.getInstance().getEsConnectionManager().retrieveAndInsertUploadListFor(collection);
				return null;
			}
		};

		mpnDownloadUploads.progressProperty().bind(collectionUploadDownloader.progressProperty());

		// Once done enable the download list and hide the loading label and circle
		collectionUploadDownloader.setOnSucceeded(event ->
		{
			this.mpnDownloadUploads.setVisible(false);
			this.mpnDownloadUploads.progressProperty().unbind();
			this.vbxDownloadList.setDisable(false);
		});

		// Add the task
		CalliopeData.getInstance().getExecutor().getQueuedExecutor().addTask(collectionUploadDownloader);
	}

	/**
	 * When we click the new collection button
	 *
	 * @param actionEvent ignored
	 */
	public void newCollectionPressed(ActionEvent actionEvent)
	{
		// Create the collection
		ImageCollection collection = new ImageCollection();
		// Create permissions for the owner
		Permission owner = new Permission();
		// Ensure that the owner has own permissions and then add it to the collection
		owner.setUsername(CalliopeData.getInstance().usernameProperty().getValue());
		owner.setRead(true);
		owner.setUpload(true);
		owner.setOwner(true);
		collection.getPermissions().add(owner);
		// Add the collection to the global collection list
		CalliopeData.getInstance().getCollectionList().add(collection);
	}

	/**
	 * When we click the delete collection button
	 *
	 * @param actionEvent consumed
	 */
	public void deleteCollectionPressed(ActionEvent actionEvent)
	{
		// Grab the selected collection
		ImageCollection selected = this.collectionListView.getSelectionModel().getSelectedItem();
		if (selected != null)
		{
			// If a collection is selected, show an alert that data may be deleted!
			CalliopeData.getInstance().getErrorDisplay().notify("Are you sure you want to delete this collection?\nDeleting this collection will result in the permanent removal of all images uploaded to CyVerse to this collection.\nAre you sure you want to continue?",
					new Action("Continue", actionEvent1 ->
					{
						// Remove the collection on the CyVerse system
						CalliopeData.getInstance().getCyConnectionManager().removeCollection(selected);

						// Remove the selected collection
						CalliopeData.getInstance().getCollectionList().remove(selected);
					}));
		} else
		{
			// If no collection is selected, show an alert
			CalliopeData.getInstance().getErrorDisplay().notify("Please select a collection from the collection list to remove.");
		}
		actionEvent.consume();
	}

	/**
	 * Called when the refresh uploads button is clicked, downloads all uploads to a collection
	 *
	 * @param actionEvent consumed
	 */
	public void refreshUploads(ActionEvent actionEvent)
	{
		// Make sure we have a selected collection
		if (this.selectedCollection.getValue() != null)
		{
			// Just resync
			this.syncUploadsForCollection(this.selectedCollection.getValue());
		}
		actionEvent.consume();
	}

	/**
	 * Clears the upload search box when the x button is pressed
	 *
	 * @param actionEvent consumed
	 */
	public void resetUploadSearch(ActionEvent actionEvent)
	{
		this.txtUploadSearch.clear();
		actionEvent.consume();
	}

	/**
	 * Indexes existing images on the CyVerse datastore
	 *
	 * @param actionEvent consumed
	 */
	public void indexExisting(ActionEvent actionEvent)
	{
		if (!CalliopeData.getInstance().getSettings().getDisablePopups())
		{
			TextInputDialog dialog = new TextInputDialog();
			dialog.setContentText(null);
			dialog.setHeaderText("Enter the path to the top level CyVerse datastore directory to recursively index\nEx: /iplant/home/dslovikosky/myUploads/myImages/");
			dialog.setTitle("Index Existing Images");

			dialog.showAndWait().ifPresent(result ->
			{
				CalliopeData.getInstance().getEsConnectionManager().refreshNeonSiteCache();
				//if (this.selectedCollection.getValue() != null)
				//	CalliopeData.getInstance().getCyConnectionManager().indexExisitingImages(this.selectedCollection.getValue(), result);
			});
		}
		else
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Popups must be enabled to see credits");
		}
		actionEvent.consume();
	}

	/**
	 * Class required because TaskProgressViewSkin$TaskCell has a cancel button that cannot be removed.
	 * This class is copy + pasted from TaskProgressViewSkin$TaskCell, so see that for documentation. Only the
	 * cancel button field was removed.
	 *
	 * @param <T> Should just be Task<?>
	 */
	private class TaskCell<T extends Task<?>> extends ListCell<T>
	{
		private ProgressBar progressBar;
		private Label titleText;
		private Label messageText;

		private T task;
		private BorderPane borderPane;

		TaskCell()
		{
			titleText = new Label();
			titleText.getStyleClass().add("task-title");

			messageText = new Label();
			messageText.getStyleClass().add("task-message");

			progressBar = new ProgressBar();
			progressBar.setMaxWidth(Double.MAX_VALUE);
			progressBar.setMaxHeight(8);
			progressBar.getStyleClass().add("task-progress-bar");

			VBox vbox = new VBox();
			vbox.setSpacing(4);
			vbox.getChildren().add(titleText);
			vbox.getChildren().add(progressBar);
			vbox.getChildren().add(messageText);

			borderPane = new BorderPane();
			borderPane.setCenter(vbox);
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}

		@Override
		public void updateIndex(int index)
		{
			super.updateIndex(index);

			/*
			 * I have no idea why this is necessary but it won't work without
			 * it. Shouldn't the updateItems method be enough?
			 */
			if (index == -1)
			{
				setGraphic(null);
				getStyleClass().setAll("task-list-cell-empty");
			}
		}

		@Override
		protected void updateItem(T task, boolean empty)
		{
			super.updateItem(task, empty);

			this.task = task;

			if (empty || task == null)
			{
				getStyleClass().setAll("task-list-cell-empty");
				setGraphic(null);
			} else
			{
				getStyleClass().setAll("task-list-cell");
				progressBar.progressProperty().bind(task.progressProperty());
				titleText.textProperty().bind(task.titleProperty());
				messageText.textProperty().bind(task.messageProperty());

				/*
				 * Really needed. The application might have used a graphic
				 * factory before and then disabled it. In this case the border
				 * pane might still have an old graphic in the left position.
				 */
				borderPane.setLeft(null);

				setGraphic(borderPane);
			}
		}
	}
}
