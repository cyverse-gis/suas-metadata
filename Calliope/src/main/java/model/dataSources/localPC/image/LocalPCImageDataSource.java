package model.dataSources.localPC.image;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.CalliopeData;
import model.dataSources.DirectoryManager;
import model.dataSources.localPC.LocalPCDataSource;
import model.image.ImageDirectory;
import model.image.ImageEntry;
import model.threading.ErrorTask;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.List;

/**
 * Data source used for reading images off of the local PC
 */
public class LocalPCImageDataSource extends LocalPCDataSource
{
	/**
	 * Constructor initializes the data source with default literals
	 */
	public LocalPCImageDataSource()
	{
		super("Local PC File", "Select specific image(s) to import on your computer", new Image(ImageEntry.class.getResource("/images/importWindow/importImageIcon.png").toString()));
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

		// Create a file chooser to let the user choose which images to import
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select Image(s)");
		// Set the directory to be in documents
		fileChooser.setInitialDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());
		// Show the dialog
		List<File> files = fileChooser.showOpenMultipleDialog(importWindow);
		// If the file chosen is a file and a directory process it
		if (files != null && !files.isEmpty())
		{
			Task<ImageDirectory> importTask = new ErrorTask<ImageDirectory>()
			{
				@Override
				protected ImageDirectory call()
				{
					this.updateProgress(0.05, 1.0);
					this.updateMessage("Loading files...");

					// Convert the files to a directory
					ImageDirectory directory = DirectoryManager.loadFiles(files);
					directory.setDataSource(LocalPCImageDataSource.this);

					this.updateProgress(0.1, 1.0);
					this.updateMessage("Removing empty directories...");

					// Remove any directories that are empty and contain no images
					DirectoryManager.removeEmptyDirectories(directory);

					// Update progress based on init progress
					this.updateMessage("Reading image metadata...");
					DoubleProperty progressProperty = new SimpleDoubleProperty();
					progressProperty.addListener((observable, oldValue, newValue) -> this.updateProgress(newValue.doubleValue() * 0.9 + 0.1, 1.0));
					DirectoryManager.initImages(directory, progressProperty);

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
