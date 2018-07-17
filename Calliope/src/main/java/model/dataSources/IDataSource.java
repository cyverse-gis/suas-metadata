package model.dataSources;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.Window;
import model.cyverse.ImageCollection;

import java.util.List;

/**
 * Interface used by any data source providing images to Calliope
 */
public interface IDataSource
{
	/**
	 * Very important method in the interface. It accepts a windows as a parameter to allow for UI thread locking popups.
	 * This method returns a task which when executed must return an image directory or null if the directory could not be
	 * retrieved for any reason.
	 *
	 * @param importWindow The window calling this method, may be locked by this method by popups if necessary
	 * @return A task that when executed pulls the image directory from the data source or null if the data source could not be retrieved
	 */
	Task<ImageDirectory> makeImportTask(Window importWindow);

	/**
	 * Very important method in the interface. It accepts a directory as a parameter and
	 * returns a task which when executed will upload the directory to the data source and index it
	 *
	 * @param imageCollection The image collection to upload the directory to
	 * @param directoryToIndex The directory to save/index
	 * @return A task that when executed saves the image directory to the data source and indexes its images into ES
	 */
	Task<Void> makeIndexTask(ImageCollection imageCollection, ImageDirectory directoryToIndex);

	/**
	 * @return Getter for the user-friendly name of the data source
	 */
	String getName();

	/**
	 * @return Getter for the user-friendly name property of the data source
	 */
	ReadOnlyStringProperty nameProperty();

	/**
	 * @return Getter for the user-friendly description of the data source
	 */
	String getDescription();

	/**
	 * @return Getter for the user-friendly description property of the data source
	 */
	ReadOnlyStringProperty descriptionProperty();

	/**
	 * @return Getter for the icon of the data source
	 */
	Image getIcon();

	/**
	 * @return Getter for the icon property of the data source
	 */
	ReadOnlyObjectProperty<Image> iconProperty();
}
