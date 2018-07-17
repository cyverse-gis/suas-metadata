package model.dataSources.localPC;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import model.CalliopeData;
import model.cyverse.ImageCollection;
import model.dataSources.IDataSource;
import model.dataSources.ImageDirectory;
import model.dataSources.ImageEntry;
import model.threading.ErrorTask;
import org.irods.jargon.core.transfer.TransferStatus;
import org.irods.jargon.core.transfer.TransferStatusCallbackListener;

import java.util.stream.Collectors;

/**
 * Data source used by the local PC
 */
public abstract class LocalPCDataSource implements IDataSource
{
	// The name of the data source
	private ReadOnlyStringWrapper name = new ReadOnlyStringWrapper();
	// The description of the data source
	private ReadOnlyStringWrapper description = new ReadOnlyStringWrapper();
	// The image representing this data source
	private ReadOnlyObjectWrapper<Image> image = new ReadOnlyObjectWrapper<>();

	/**
	 * Constructor sets the name, description, and image of the data source
	 *
	 * @param name The name of the data source
	 * @param description The description of the data source
	 * @param image The image of the data source
	 */
	public LocalPCDataSource(String name, String description, Image image)
	{
		this.name.setValue(name);
		this.description.setValue(description);
		this.image.setValue(image);
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
	public Task<Void> makeIndexTask(ImageCollection imageCollection, ImageDirectory directoryToIndex)
	{
		// Make sure we've got a valid directory
		boolean validDirectory = true;
		// Each image must have a location tagged
		for (ImageEntry imageEntry : directoryToIndex.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry).map(imageContainer -> (ImageEntry) imageContainer).collect(Collectors.toList()))
			if (imageEntry.getLocationTaken() == null)
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
					// Upload images to CyVerse, we give it a transfer status callback so that we can show the progress
					CalliopeData.getInstance().getCyConnectionManager().uploadAndIndexImages(imageCollection, directoryToIndex, new TransferStatusCallbackListener()
					{
						@Override
						public FileStatusCallbackResponse statusCallback(TransferStatus transferStatus)
						{
							// Set the upload progress in the directory we get a callback
							Platform.runLater(() -> directoryToIndex.setUploadProgress(transferStatus.getBytesTransfered() / (double) transferStatus.getTotalSize()));
							// Set the upload progress whenever we get a callback
							updateProgress((double) transferStatus.getBytesTransfered(), (double) transferStatus.getTotalSize());
							return FileStatusCallbackResponse.CONTINUE;
						}
						// Ignore this status callback
						@Override
						public void overallStatusCallback(TransferStatus transferStatus) {}
						// Ignore this as well
						@Override
						public CallbackResponse transferAsksWhetherToForceOperation(String irodsAbsolutePath, boolean isCollection)
						{
							return CallbackResponse.YES_FOR_ALL;
						}
					}, messageCallback);
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
