package model.dataSources.localPC.directory;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import model.CalliopeData;
import model.dataSources.DirectoryManager;
import model.image.ImageDirectory;
import model.image.ImageEntry;
import model.dataSources.localPC.LocalPCDataSource;
import model.threading.ErrorTask;

import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * Data source used for reading a directory of images off of the local PC
 */
public class LocalPCDirectoryDataSource extends LocalPCDataSource
{
	/**
	 * Constructor initializes the data source with default literals
	 */
	public LocalPCDirectoryDataSource()
	{
		super("Local PC Directory", "Select a directory to recursively import images from on your computer", new Image(ImageEntry.class.getResource("/images/importWindow/importDirectoryIcon.png").toString()));
	}

	/**
	 * Accepts a windows as a parameter to allow for UI thread locking popups.
	 * This method returns a task which when executed must return an image directory or null if the directory could not be
	 * retrieved for any reason.
	 *
	 * @param importWindow The window calling this method, may be locked by this method by popups if necessary
	 * @return A task that when executed pulls the image directory from the data source or null if the data source could not be retrieved
	 */
	@Override
	public Task<ImageDirectory> makeImportTask(Window importWindow)
	{
		// If our Exiftool was not loaded successfully we can't import images, so throw an error here
		if (!CalliopeData.getInstance().getMetadataManager().isExifToolFound())
		{
			CalliopeData.getInstance().getErrorDisplay().notify("ExifTool is required to read image metadata. See the bottom of the settings tab for installation instructions");
			return null;
		}

		// Create a directory chooser to let the user choose where to get the images from
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Select Folder with Images");
		// Set the directory to be in documents
		directoryChooser.setInitialDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());
		// Show the dialog
		File file = directoryChooser.showDialog(importWindow);
		// If the file chosen is a file and a directory process it
		if (file != null && file.isDirectory())
		{
			Task<ImageDirectory> importTask = new ErrorTask<ImageDirectory>()
			{
				@Override
				protected ImageDirectory call()
				{
					final Long MAX_WORK = 2L;

					this.updateProgress(1, MAX_WORK);
					this.updateMessage("Loading directory...");

					// Convert the file to a recursive image directory data structure
					ImageDirectory directory = DirectoryManager.loadDirectory(file);
					directory.setDataSource(LocalPCDirectoryDataSource.this);

					this.updateProgress(2, MAX_WORK);
					this.updateMessage("Removing empty directories...");

					// Remove any directories that are empty and contain no images
					DirectoryManager.removeEmptyDirectories(directory);

					return directory;
				}
			};
			importTask.setOnSucceeded(event ->
			{
				// Add the directory to the image tree
				CalliopeData.getInstance().getImageTree().addChild(importTask.getValue());
			});

			return importTask;
		}

		return null;
	}
}
