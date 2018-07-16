package model.dataSources.cyverseDataStore;

import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.stage.Window;
import model.CalliopeData;
import model.dataSources.DirectoryManager;
import model.dataSources.IDataSource;
import model.dataSources.ImageDirectory;
import model.dataSources.ImageEntry;
import model.threading.ErrorTask;

import java.util.Optional;

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
