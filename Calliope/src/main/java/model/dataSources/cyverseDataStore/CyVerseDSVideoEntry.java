package model.dataSources.cyverseDataStore;

import com.thebuzzmedia.exiftool.Tag;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.image.Image;
import model.CalliopeData;
import model.image.ImageEntry;
import model.threading.ErrorTask;
import org.irods.jargon.core.pub.io.IRODSFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Class representing a CyVerse datastore image entry
 */
public class CyVerseDSImageEntry extends ImageEntry
{
	// The icon to use for all downloaded untagged images
	private static final Image DEFAULT_CLOUD_IMAGE_ICON = new Image(ImageEntry.class.getResource("/images/importWindow/imageCloudIcon.png").toString());
	// The icon to use for an un-downloaded images
	private static final Image NO_DOWNLOAD_CLOUD_IMAGE_ICON = new Image(ImageEntry.class.getResource("/images/importWindow/imageCloudIconNotDownloaded.png").toString());

	// Flag telling us if the metadata on this image was retrieved or not
	private ReadOnlyBooleanWrapper wasMetadataRetrieved = new ReadOnlyBooleanWrapper(false);
	// Flag telling us if the metadata retrieval is currently in progress or not
	private Boolean metadataRetrievalInProgress = false;

	/**
	 * Create a new image entry with an image file
	 *
	 * @param file The file (must be an image file)
	 */
	public CyVerseDSImageEntry(File file)
	{
		super(file);
		// The metadata is editable if it has been retrieved
		this.metadataEditable.bind(this.wasMetadataRetrieved);
		this.treeIconProperty().setValue(NO_DOWNLOAD_CLOUD_IMAGE_ICON);
	}

	/**
	 * Instead of retrieving a local file we download an iRODS file and read that instead
	 *
	 * @return A buffered image representing this CyVerse datastore entry
	 */
	@Override
	protected BufferedImage retrieveRawImage()
	{
		return CalliopeData.getInstance().getCyConnectionManager().readIRODSImage(this.getFile().getAbsolutePath());
	}

	/**
	 * Reading this file's metadata just causes it to pull the metadata from the cloud
	 */
	@Override
	public void readFileMetadataFromImage()
	{
		this.pullMetadataFromCyVerse();
	}

	/**
	 * Called to pull the metadata for this image file from the cloud
	 */
	void pullMetadataFromCyVerse()
	{
		// If we have already retrieved metadata or are currently retrieving metadata we just return
		if (this.wasMetadataRetrieved.getValue() || this.metadataRetrievalInProgress)
			return;

		// We are retrieving metadata so set the flag to true
		metadataRetrievalInProgress = true;

		// Create a task that is used to pull the image metadata
		ErrorTask<Map<Tag, String>> metadataPullTask = new ErrorTask<Map<Tag, String>>()
		{
			@Override
			protected Map<Tag, String> call() throws IOException
			{
				// Convert the iRODS file to a local image file
				File localFile = CalliopeData.getInstance().getCyConnectionManager().remoteToLocalImageFile((IRODSFile) CyVerseDSImageEntry.this.getFile());
				// Read this image's metadata
				Map<Tag, String> metadata = CalliopeData.getInstance().getMetadataManager().readImageMetadata(localFile);
				// Delete our local file now that we've read the metadata
				localFile.delete();
				// Return the data
				return metadata;
			}
		};
		// When the task succeeds...
		metadataPullTask.setOnSucceeded(event ->
		{
			// Read the metadata map returned from the task and store it
			this.readFileMetadataFromMap(metadataPullTask.getValue());
			// Update our flags
			this.wasMetadataRetrieved.setValue(true);
			metadataRetrievalInProgress = false;
			this.treeIconProperty().setValue(DEFAULT_CLOUD_IMAGE_ICON);
			this.buildAndStoreIcon();
		});
		// If the task fails, we set our flag to false so we can attempt the metadata retrieval again
		metadataPullTask.setOnFailed(event -> metadataRetrievalInProgress = false);
		// Perform immediate execution of this task
		CalliopeData.getInstance().getExecutor().getImmediateExecutor().addTask(metadataPullTask);
	}

	///
	/// Getters/Setters
	///

	public boolean isMetadataEditable()
	{
		return metadataEditable.getValue();
	}

	public BooleanProperty metadataEditableProperty()
	{
		return this.metadataEditable;
	}

	public ReadOnlyBooleanProperty metadataWasRetrieved()
	{
		return this.wasMetadataRetrieved.getReadOnlyProperty();
	}

	public boolean wasMetadataRetrieved()
	{
		return this.wasMetadataRetrieved.getValue();
	}
}
