package model.util;

import javafx.concurrent.Task;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.CalliopeData;
import model.image.DirectoryManager;
import model.image.ImageDirectory;
import model.image.ImageEntry;
import model.threading.ErrorTask;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * An enum containing a list of data sources
 */
public enum ImportableDataSources
{
	// Local file on the PC's disk or external USB device
	LocalFile("Local PC File", "Select specific image(s) to import into the program from your computer", new Image(ImageEntry.class.getResource("/images/importWindow/importImageIcon.png").toString()))
	{
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
						final Long MAX_WORK = 2L;

						this.updateProgress(1, MAX_WORK);
						this.updateMessage("Loading files...");

						// Convert the files to a directory
						ImageDirectory directory = DirectoryManager.loadFiles(files);

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
	},
	LocalDirectory("Local PC Directory", "Select a directory to recursively import images from into the program from your computer", new Image(ImageEntry.class.getResource("/images/importWindow/importDirectoryIcon.png").toString()))
	{
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
	},
	CyVerseDataStore("CyVerse Data Store", "Input a directory on the CyVerse Data Store to import and use within the program", new Image(ImageEntry.class.getResource("/images/importWindow/importCyVerseIcon.png").toString()))
	{
		@Override
		public Task<ImageDirectory> makeImportTask(Window importWindow)
		{
			// Make sure popups are enabled
			if (!CalliopeData.getInstance().getSettings().getDisablePopups())
			{
				// We use a text input dialog to read the name of the directory to import
				TextInputDialog dialog = new TextInputDialog();
				// Set the required fields:
				dialog.setContentText(null);
				dialog.setHeaderText("Enter the path to the top level CyVerse datastore directory to recursively index\nEx: /iplant/home/dslovikosky/myUploads/myImages/");
				dialog.setTitle("Index Existing Images");

				// Show the dialog and store the result
				Optional<String> result = dialog.showAndWait();
				if (result.isPresent())
				{
					// If we got a result, store it into a string
					String pathToFiles = result.get();
					// Now that we have a result, we move to a different thread to perform the directory reading
					ErrorTask<ImageDirectory> indexExistingTask = new ErrorTask<ImageDirectory>()
					{
						@Override
						protected ImageDirectory call()
						{
							// Update the message used by the task to display progress
							this.updateMessage("Parsing existing images in preparation to index...");
							// Ask our CyVerse connection manager to read the directory
							ImageDirectory imageDirectory = CalliopeData.getInstance().getCyConnectionManager().prepareExistingImagesForIndexing(pathToFiles);
							// Remove any empty directories
							DirectoryManager.removeEmptyDirectories(imageDirectory);
							return imageDirectory;
						}
					};
					indexExistingTask.setOnSucceeded(event ->
					{
						// After the task completes, we check if the return value is null and if it isn't we add it to our image tree
						if (indexExistingTask.getValue() != null)
							CalliopeData.getInstance().getImageTree().addChild(indexExistingTask.getValue());
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
	};

	private String name;
	private String description;
	private Image icon;

	/**
	 * Constructor takes the name of the source, the description of the source, and the icon of the source
	 *
	 * @param name The user-friendly name of the data source
	 * @param description The user-friendly description of the data source
	 * @param icon The image icon representing the data source
	 */
	ImportableDataSources(String name, String description, Image icon)
	{
		this.name = name;
		this.description = description;
		this.icon = icon;
	}

	/**
	 * @return Getter for name of the data source
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * @return Getter for icon of the data source
	 */
	public Image getIcon()
	{
		return this.icon;
	}

	/**
	 * @return Getter for description of the data source
	 */
	public String getDescription()
	{
		return this.description;
	}

	/**
	 * Abstract method that returns a task that retrieves the image directory from a data source
	 *
	 * @param importWindow The current UI window that is open
	 * @return A task that, when run, returns an image directory
	 */
	public abstract Task<ImageDirectory> makeImportTask(Window importWindow);
}
