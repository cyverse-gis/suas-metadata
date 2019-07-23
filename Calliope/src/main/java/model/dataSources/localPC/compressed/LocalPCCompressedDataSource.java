package model.dataSources.localPC.compressed;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
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
 * Data source used for reading compressed files off of the local PC
 *
 * @author Jackson but I copied most of it from David's Image and Directory classes
 */
public class LocalPCCompressedDataSource extends LocalPCDataSource
{
    /**
     * Constructor initializes the data source with default literals
     */
    public LocalPCCompressedDataSource()
    {
        super("Local PC Compressed", "Select compressed archives (.zip, .tar.gz) of files to import", new Image(ImageEntry.class.getResource("/images/importWindow/importImageIcon.png").toString()));
    }

    /**
     * Accepts a windows as a parameter to allow for UI thread locking popups.
     * This method returns a task which when executed must return a file directory or null if the directory could not be
     * retrieved for any reason.
     *
     * @param importWindow The window calling this method, may be locked by this method by popups if necessary
     * @return A task that when executed pulls the file directory from the data source or null if the data source could not be retrieved
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

        // Create a file chooser to let the user choose which files to import
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Compressed File(s)");
        // Set the directory to be in the user's default directory
        fileChooser.setInitialDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());
        // Show the dialog
        List<File> files = fileChooser.showOpenMultipleDialog(importWindow);
        // If the file chosen is a file and not empty process it
        if (files != null && !files.isEmpty())
        {
            Task<ImageDirectory> importTask = new ErrorTask<ImageDirectory>()
            {
                @Override
                protected ImageDirectory call()
                {
                    this.updateProgress(0.05, 1.0);
                    this.updateMessage("Decompressing files...");

                    // Convert the compressed files to a directory
                    // This is where non-compressed files are removed and where extraction occurs
                    ImageDirectory directory = DirectoryManager.loadCompressed(files);
                    directory.setDataSource(LocalPCCompressedDataSource.this);

                    this.updateProgress(0.1, 1.0);
                    this.updateMessage("Removing empty directories...");

                    // Remove any directories that are empty and contain no files
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
                // Ensure that the directory we created was a valid directory.
                // TODO: Would this fit better elsewhere? ie, can we make importTask fail?
                if(importTask.getValue().isValid())
                    // Add the directory to the image tree
                    CalliopeData.getInstance().getImageTree().addChild(importTask.getValue());
            });

            return importTask;
        }
        return null;
    }
}
