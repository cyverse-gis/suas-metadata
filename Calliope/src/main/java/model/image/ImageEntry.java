package model.image;

import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import model.CalliopeData;
import model.settings.MetadataCustomItem;
import model.site.Site;
import model.threading.ErrorTask;
import model.util.CustomPropertyItem;
import org.apache.commons.lang3.exception.ExceptionUtils;
//import org.apache.commons.lang.exception.ExceptionUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A class representing an image file
 * 
 * @author David Slovikosky
 */
public class ImageEntry extends DataContainer
{
	private static final DateTimeFormatter DATE_FORMAT_FOR_DISK = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

	// If the image entry's metadata is currently ready to be edited
	protected transient final SimpleBooleanProperty metadataEditable = new SimpleBooleanProperty(true);

	// Flag that tells us if we've pulled the icon or not
	private transient final AtomicBoolean gotIcon = new AtomicBoolean(false);

	/**
	 * Create a new image entry with an image file
	 * 
	 * @param file
	 *            The file (must be an image file)
	 */
	public ImageEntry(File file)
	{
		this.imageFile.setValue(file);
	}

	/**
	 * Reads the file metadata and initializes fields
	 */
	public void readFileMetadataFromImage()
	{
		try
		{
			// Read the metadata off of the image
			Map<Tag, String> imageMetadataMap = CalliopeData.getInstance().getMetadataManager().readImageMetadata(this.getFile());
			this.readFileMetadataFromMap(imageMetadataMap);
		}
		catch (Exception e)
		{
			// If reading the metadata fails in any way, print an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error reading image metadata for file " + this.getFile().getName() + "!\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Given a map of Tag -> String metadata, this method stores the given metadata into the image
	 *
	 * @param imageMetadataMap A mapping of tag -> string with the image's metadata
	 */
	protected void readFileMetadataFromMap(Map<Tag, String> imageMetadataMap)
	{
		// Constant meaning that the metadata attribute was not given in the metadata
		final String UNSPECIFIED = "Unspecified";
		// Clear the list of raw metadata
		this.rawMetadata.clear();
		// For each metadata tag, add an item to the list
		for (Map.Entry<Tag, String> entry : imageMetadataMap.entrySet())
			this.rawMetadata.add(new MetadataCustomItem(entry.getKey().getName(), entry.getValue()));
		// Sort the raw metadata by name for convenience
		this.rawMetadata.sort(Comparator.comparing(CustomPropertyItem::getName));

		// Now we parse the raw metadata into something useful to index

		// Starting with date taken, convert the raw date taken as a string into an object
		this.dateTaken.setValue(LocalDateTime.parse(imageMetadataMap.getOrDefault(StandardTag.DATE_TIME_ORIGINAL, LocalDateTime.now().format(DATE_FORMAT_FOR_DISK)), DATE_FORMAT_FOR_DISK));
		// Next convert the lat/long/altitude into a location object
		this.positionTaken.setValue(new Position(
				Double.parseDouble(imageMetadataMap.getOrDefault(StandardTag.GPS_LATITUDE, "0")),
				Double.parseDouble(imageMetadataMap.getOrDefault(StandardTag.GPS_LONGITUDE, "0")),
				Double.parseDouble(imageMetadataMap.getOrDefault(StandardTag.GPS_ALTITUDE, "0"))));
		// Then store the maker and model fields separately
		this.droneMaker.setValue(imageMetadataMap.getOrDefault(StandardTag.MAKE, UNSPECIFIED));
		this.cameraModel.setValue(imageMetadataMap.getOrDefault(MetadataManager.CustomTags.CAMERA_MODEL_NAME, UNSPECIFIED));
		// Speed can be stored as a 3D vector, so convert 3 doubles to a vector
		this.speed.setValue(new Vector3(
				Double.parseDouble(imageMetadataMap.getOrDefault(MetadataManager.CustomTags.SPEED_X, "0")),
				Double.parseDouble(imageMetadataMap.getOrDefault(MetadataManager.CustomTags.SPEED_Y, "0")),
				Double.parseDouble(imageMetadataMap.getOrDefault(MetadataManager.CustomTags.SPEED_Z, "0"))));
		// Rotation can be stored as a 3D vector, so convert 3 doubles to a vector
		this.rotation.setValue(new Vector3(
				Double.parseDouble(imageMetadataMap.getOrDefault(MetadataManager.CustomTags.ROLL, "0")),
				Double.parseDouble(imageMetadataMap.getOrDefault(MetadataManager.CustomTags.PITCH, "0")),
				Double.parseDouble(imageMetadataMap.getOrDefault(MetadataManager.CustomTags.YAW, "0"))));

		// Grab the position this image was taken at
		Position position = this.positionTaken.getValue();
		// Set the altitude to the position's elevation - the ground elevation position
		this.altitude.setValue(position.getElevation() - CalliopeData.getInstance().getElevationData().getGroundElevation(position.getLatitude(), position.getLongitude()));
		// Store the file type, focal length, width, and height
		this.fileType.setValue(imageMetadataMap.getOrDefault(StandardTag.FILE_TYPE, UNSPECIFIED));
		this.focalLength.setValue(Double.parseDouble(imageMetadataMap.getOrDefault(StandardTag.FOCAL_LENGTH, "0")));
		this.width.setValue(Double.parseDouble(imageMetadataMap.getOrDefault(StandardTag.IMAGE_WIDTH, "0")));
		this.height.setValue(Double.parseDouble(imageMetadataMap.getOrDefault(StandardTag.IMAGE_HEIGHT, "0")));

		// Store all the metadata as one string
		this.allMetadata.setValue(imageMetadataMap.getOrDefault(MetadataManager.CustomTags.ALL_METADATA, UNSPECIFIED));
	}

	/**
	 * Downloads the image file into a buffered image
	 *
	 * @return A buffered image representing the image file
	 */
	protected BufferedImage retrieveRawImage()
	{
		try
		{
			// Read the file into an bufferedImage
			return ImageIO.read(this.getFile());
		}
		catch (IOException e)
		{
			// If an error occurs, print it out
			CalliopeData.getInstance().getErrorDisplay().notify("Error loading image file '" + this.getFile().getAbsolutePath() + "'\n" + ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	/**
	 * Creates a displayable image from a local file by reading it off the disk
	 *
	 * @return An image in memory representing the file on disk
	 */
	public Image buildIntoImage()
	{
		// Read the file into an bufferedImage
		BufferedImage bufferedImage = this.retrieveRawImage();
		// Return null if the bufferedImage did not read properly
		if (bufferedImage == null)
			return null;
		// Can't use 'new Image(file.toURI().toString())' because it doesn't support tiffs, sad day
		return SwingFXUtils.toFXImage(bufferedImage, null);
	}

	/**
	 * Downloads the image entry, stores a down-scaled version of the icon, and discards the gotIcon image file
	 */
	public void buildAndStoreIcon()
	{
		if (this.gotIcon.get())
			return;

		this.gotIcon.set(true);

		// Thread this off...
		Task<Image> iconBuilder = new ErrorTask<Image>()
		{
			@Override
			protected Image call()
			{
				// Read the file into an bufferedImage
				BufferedImage bufferedImage = ImageEntry.this.retrieveRawImage();
				// Make sure the file was readable
				if (bufferedImage != null)
				{
					// Downsize the image to icon size
					java.awt.Image scaledInstance = bufferedImage.getScaledInstance(32, 32, BufferedImage.SCALE_REPLICATE);

					// Grab the width and height of the image
					Integer width = scaledInstance.getWidth(null);
					Integer height = scaledInstance.getHeight(null);

					// Convert the java.awt.Image to a BufferedImage, and then to a JavaFX Image
					BufferedImage icon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

					// Grab the graphics of the image and draw the icon to it
					Graphics graphics = icon.getGraphics();
					graphics.drawImage(scaledInstance, 0, 0, null);
					graphics.dispose();

					// Store the result
					return SwingFXUtils.toFXImage(icon, null);
				}
				return null;
			}
		};
		// Once this finishes, set the icon
		iconBuilder.setOnSucceeded(event -> this.icon.setValue(iconBuilder.getValue()));
		// Execute the
		CalliopeData.getInstance().getExecutor().getBackgroundExecutor().addTask(iconBuilder);
	}

	///
	/// Getters/Setters
	///

	@Override
	public ObjectProperty<Image> treeIconProperty()
	{
		return this.icon;
	}

	@Override
	public File getFile()
	{
		return this.imageFile.getValue();
	}

	public ObjectProperty<File> fileProperty()
	{
		return this.imageFile;
	}

	// TODO: Is this needed? I can't think of an instance where we would want to set the metadata read to something else...

	public void setAllMetadata(String allMeta)
	{
		this.allMetadata.setValue(allMeta);
	}

	public String getAllMetadata()
	{
		return this.allMetadata.getValue();
	}

	public StringProperty allMetadataProperty()
	{
		return this.fileType;
	}

	@Override
	public void setSiteTaken(Site siteTaken)
	{
		this.siteTaken.add(siteTaken);
	}
}
