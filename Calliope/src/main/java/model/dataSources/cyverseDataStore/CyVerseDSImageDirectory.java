package model.dataSources.cyverseDataStore;

import javafx.scene.image.Image;
import model.dataSources.ImageDirectory;
import model.dataSources.ImageEntry;
import org.irods.jargon.core.pub.io.IRODSFile;

import java.io.File;

/**
 * Class representing a CyVerse DataStore directory ready to be indexed
 */
public class CyVerseDSImageDirectory extends ImageDirectory
{
	// The default cloud directory image to display
	private static final Image DEFAULT_CLOUD_DIR_IMAGE = new Image(ImageEntry.class.getResource("/images/importWindow/cloudDirectoryIcon.png").toString());

	/**
	 * Construct an image directoryProperty
	 *
	 * @param directory The file that represents the directoryProperty
	 */
	public CyVerseDSImageDirectory(File directory)
	{
		super(directory);
		this.treeIconProperty().setValue(DEFAULT_CLOUD_DIR_IMAGE);
	}

	/**
	 * Getter for CyVerse file
	 *
	 * @return The regular file casted to an iRODS file
	 */
	public IRODSFile getCyverseFile()
	{
		return (IRODSFile) this.getFile();
	}
}
