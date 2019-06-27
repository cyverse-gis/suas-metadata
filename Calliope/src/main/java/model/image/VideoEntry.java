package model.image;

import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.SnapshotResult;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Callback;
import model.CalliopeData;
import model.settings.MetadataCustomItem;
import model.site.Site;
import model.threading.ErrorTask;
import model.util.CustomPropertyItem;
import org.apache.commons.lang.exception.ExceptionUtils;

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
public class VideoEntry extends DataContainer
{
	private static final DateTimeFormatter DATE_FORMAT_FOR_DISK = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

	// Flag that tells us if we've pulled the icon or not
	private transient final AtomicBoolean gotIcon = new AtomicBoolean(false);

	/**
	 * Create a new video entry with a video file
	 *
	 * @param file
	 *            The file (must be a video file)
	 */
	public VideoEntry(File file)
	{
		this.imageFile.setValue(file);
	}

	/**
	 * Reads the file metadata and initializes fields
	 */
	public void readFileMetadataFromVideo()
	{
		try
		{
			// Read the metadata off of the video
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
	}

	/**
	 * Creates a displayable video from a local file by reading it off the disk
	 *
	 * @return A video in memory representing the file on disk
	 */
	public Media buildIntoMedia()
	{
		File file = this.getFile();
		return new Media(file.toURI().toString());
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
				// Use ffmpeg here to get an icon?
				// Nope! Try to use MediaView's snapshot instead
				Media temp = VideoEntry.this.buildIntoMedia();
				WritableImage icon = new WritableImage(32, 32);
				MediaPlayer mp = new MediaPlayer(temp);
				MediaView mv = new MediaView(mp);
				mp.play();
				Platform.runLater(() -> {
							mv.snapshot(result -> {
								Image image = result.getImage();
								if (image == null) {
									return null;
								}
								PixelWriter writer = icon.getPixelWriter();
								writer.setPixels(0, 0, temp.getWidth(), temp.getHeight(), image.getPixelReader(), 0, 0);
								return null;
							}, null, null);
						});
				mp.stop();
				return icon;
			}
		};
		// Once this finishes, set the icon
		iconBuilder.setOnSucceeded(event -> this.icon.setValue(iconBuilder.getValue()));
		// Execute the task
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

	@Override
	public void setSiteTaken(Site siteTaken)
	{
		this.siteTaken.setValue(siteTaken);
	}
}
