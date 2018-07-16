package model.dataSources.cyverseDataStore;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import model.CalliopeData;
import model.dataSources.ImageEntry;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class representing a CyVerse datastore image entry
 */
public class CyVerseDSImageEntry extends ImageEntry
{
	/**
	 * Create a new image entry with an image file
	 *
	 * @param file The file (must be an image file)
	 */
	public CyVerseDSImageEntry(File file)
	{
		super(file);
	}

	/**
	 * Creates a displayable image from a CyVerse IRODSFile by downloading it using a stream
	 *
	 * @return An image in memory representing the file on CyVerse
	 */
	@Override
	public Image buildDisplayableImage()
	{
		// Download and store the image data from the iRODS file on CyVerse
		BufferedImage bufferedImage = CalliopeData.getInstance().getCyConnectionManager().readIRODSImage(this.getFile().getAbsolutePath());
		// If the data is not null, return the image converted to a JavaFX image
		if (bufferedImage != null)
			return SwingFXUtils.toFXImage(bufferedImage, null);
		else
			return null;
	}
}
