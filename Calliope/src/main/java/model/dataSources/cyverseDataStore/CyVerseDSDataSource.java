package model.dataSources.cyverseDataStore;

import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.stage.Window;
import model.CalliopeData;
import model.cyverse.ImageCollection;
import model.dataSources.DirectoryManager;
import model.dataSources.IDataSource;
import model.dataSources.UploadedEntry;
import model.image.DataDirectory;
import model.image.ImageEntry;
import model.image.VideoEntry;
import model.threading.ErrorTask;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data source used by the CyVerse datastore
 */
public class CyVerseDSDataSource implements IDataSource
{
	// The name of the data source
	private ReadOnlyStringWrapper name = new ReadOnlyStringWrapper("CyVerse Data Store");
	// The description of the data source
	private ReadOnlyStringWrapper description = new ReadOnlyStringWrapper("Input a directory on the CyVerse Data Store to import and use within the program");
	// The image representing this data source
	private ReadOnlyObjectWrapper<Image> image = new ReadOnlyObjectWrapper<>(new Image(ImageEntry.class.getResource("/images/importWindow/importCyVerseIcon.png").toString()));

	/**
	 * Accepts a windows as a parameter to allow for UI thread locking popups.
	 * This method returns a task which when executed must return an image directory or null if the directory could not be
	 * retrieved for any reason.
	 *
	 * @param importWindow The window calling this method, may be locked by this method by popups if necessary
	 * @return A task that when executed pulls the image directory from the data source or null if the data source could not be retrieved
	 */
	@Override
	public Task<DataDirectory> makeImportTask(Window importWindow)
	{
		// Make sure popups are enabled
		if (!CalliopeData.getInstance().getSettings().getDisablePopups())
		{
			// We use a text input dialog to read the name of the directory to import
			TextInputDialog dialog = new TextInputDialog();
			// Set the required fields:
			dialog.setContentText(null);
			dialog.setHeaderText("Enter the path to the top level CyVerse Data Store directory to recursively index\nEx: /iplant/home/dslovikosky/myUploads/myImages/");
			dialog.setTitle("Index Existing Images");

			// Show the dialog and store the result
			Optional<String> result = dialog.showAndWait();
			if (result.isPresent())
			{
				// If we got a result, store it into a string
				String pathToFiles = result.get();
				// Now that we have a result, we move to a different thread to perform the directory reading
				ErrorTask<DataDirectory> indexExistingTask = new ErrorTask<DataDirectory>()
				{
					@Override
					protected DataDirectory call()
					{
						// Update the message used by the task to display progress
						this.updateMessage("Searching for image files in directory...");
						// Ask our CyVerse connection manager to read the directory
						CyVerseDSDataDirectory imageDirectory = CalliopeData.getInstance().getCyConnectionManager().prepareExistingImagesForIndexing(pathToFiles);
						// Will be null if the file does not exist
						if (imageDirectory != null)
						{
							// Remove any empty directories
							DirectoryManager.removeEmptyDirectories(imageDirectory);
							// Update progress based on init progress
							this.updateMessage("Reading image metadata...");
							DoubleProperty progressProperty = new SimpleDoubleProperty();
							progressProperty.addListener((observable, oldValue, newValue) -> this.updateProgress(newValue.doubleValue(), 1.0));
							DirectoryManager.initImages(imageDirectory, progressProperty);
							// Go over each image entry and queue its download
							imageDirectory.flattened().filter(imageContainer -> imageContainer instanceof CyVerseDSImageEntry).forEach(imageContainer ->
							{
								CyVerseDSImageEntry cyVerseDSImageEntry = (CyVerseDSImageEntry) imageContainer;
								cyVerseDSImageEntry.pullMetadataFromCyVerse();
							});
							imageDirectory.setDataSource(CyVerseDSDataSource.this);
						}
						return imageDirectory;
					}
				};
				indexExistingTask.setOnSucceeded(event ->
				{
					// After the task completes, we check if the return value is null and if it isn't we add it to our image tree
					if (indexExistingTask.getValue() != null)
						CalliopeData.getInstance().getImageTree().addChild(indexExistingTask.getValue());
					else
						CalliopeData.getInstance().getErrorDisplay().notify("Could not find the directory specified");
				});
				return indexExistingTask;
			}
		}
		else
		{
			// Print out an error if popups are not enabled
			CalliopeData.getInstance().getErrorDisplay().notify("Popups must be enabled to see credits");
		}
		return null;
	}

	/**
	 * Very important method in the interface. It accepts a directory as a parameter and
	 * returns a task which when executed will upload the directory to the data source and index it
	 *
	 * @param imageCollection The image collection to upload the directory to
	 * @param directoryToIndex The directory to save/index
	 * @return A task that when executed saves the image directory to the data source and indexes its images into ES
	 */
	@Override
	public Task<Void> makeIndexTask(ImageCollection imageCollection, DataDirectory directoryToIndex)
	{
		// Make sure we've got a valid directory
		boolean validDirectory = true;
		// Each image must have a location tagged
		for (CyVerseDSImageEntry imageEntry : directoryToIndex.flattened().filter(imageContainer -> imageContainer instanceof CyVerseDSImageEntry).map(imageContainer -> (CyVerseDSImageEntry) imageContainer).collect(Collectors.toList()))
			// All images must a) be downloaded and b) have a valid location taken
			if (!imageEntry.wasMetadataRetrieved() || imageEntry.getPositionTaken() == null)
			{
				validDirectory = false;
				break;
			}

		for (CyVerseDSVideoEntry videoEntry : directoryToIndex.flattened().filter(imageContainer -> imageContainer instanceof CyVerseDSVideoEntry).map(imageContainer -> (CyVerseDSVideoEntry) imageContainer).collect(Collectors.toList()))
			// All images must a) be downloaded and b) have a valid location taken
			if (!videoEntry.wasMetadataRetrieved() || videoEntry.getPositionTaken() == null)
			{
				validDirectory = false;
				break;
			}

		// If we have a valid directory, perform the upload
		if (validDirectory)
		{
			// Set the upload to 0% so that we don't edit it anymore
			directoryToIndex.setUploadProgress(0.0);
			// Create an upload task
			Task<Void> uploadTask = new ErrorTask<Void>()
			{
				@Override
				protected Void call()
				{
					// Update the progress
					this.updateProgress(0, 1);

					// Create a string property used as a callback
					StringProperty messageCallback = new SimpleStringProperty("");
					this.updateMessage("Uploading image directory " + directoryToIndex.getFile().getName() + " to CyVerse.");
					messageCallback.addListener((observable, oldValue, newValue) -> this.updateMessage(newValue));
					UploadedEntry uploadedEntry = new UploadedEntry(
							CalliopeData.getInstance().getUsername(),
							LocalDateTime.now(),
							Math.toIntExact(directoryToIndex.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry).count()),
							Math.toIntExact(directoryToIndex.flattened().filter(imageContainer -> imageContainer instanceof VideoEntry).count()),
							directoryToIndex.getFile().getAbsolutePath(),
							CyVerseDSDataSource.this.getName());
					// Upload images to CyVerse, we give it a transfer status callback so that we can show the progress
					CalliopeData.getInstance().getEsConnectionManager().indexImages(directoryToIndex, uploadedEntry, imageCollection.getID().toString(), imageEntry -> imageEntry.getFile().getAbsolutePath());
					return null;
				}
			};
			// When the upload finishes, we enable the upload button
			uploadTask.setOnSucceeded(event ->
			{
				directoryToIndex.setUploadProgress(-1);
				// Remove the directory because it's uploaded now
				CalliopeData.getInstance().getImageTree().removeChildRecursive(directoryToIndex);
			});
			uploadTask.setOnCancelled(event -> directoryToIndex.setUploadProgress(-1));
			return uploadTask;
		}
		else
		{
			// If an invalid directory is selected, show an alert
			CalliopeData.getInstance().getErrorDisplay().notify("An image in the directory (" + directoryToIndex.getFile().getName() + ") you selected does not have a location. Please ensure all images are tagged with a location!");
		}
		return null;
	}

	///
	/// Getters
	///

	@Override
	public String getName()
	{
		return this.name.getValue();
	}

	@Override
	public ReadOnlyStringProperty nameProperty()
	{
		return this.name.getReadOnlyProperty();
	}

	@Override
	public String getDescription()
	{
		return this.description.getValue();
	}

	@Override
	public ReadOnlyStringProperty descriptionProperty()
	{
		return this.description.getReadOnlyProperty();
	}

	@Override
	public Image getIcon()
	{
		return this.image.getValue();
	}

	@Override
	public ReadOnlyObjectProperty<Image> iconProperty()
	{
		return this.image.getReadOnlyProperty();
	}
}
