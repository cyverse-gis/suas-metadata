package model.image;

import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import model.CalliopeData;
import model.location.Location;
import model.neon.BoundedSite;
import model.util.MetadataUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


/**
 * A class representing an image file
 * 
 * @author David Slovikosky
 */
public class ImageEntry extends ImageContainer
{
	private static final DateTimeFormatter DATE_FORMAT_FOR_DISK = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

	// The icon to use for all images at the moment
	private static final Image DEFAULT_IMAGE_ICON = new Image(ImageEntry.class.getResource("/images/importWindow/imageIcon.png").toString());
	// The icon to use for all location only tagged images at the moment
	private static final Image NEON_IMAGE_ICON = new Image(ImageEntry.class.getResource("/images/importWindow/imageIconNeon.png").toString());

	// A property to wrap the currently selected image property. Must not be static!
	transient final ObjectProperty<Image> selectedImage = new SimpleObjectProperty<>(DEFAULT_IMAGE_ICON);
	// The actual file 
	private final ObjectProperty<File> imageFile = new SimpleObjectProperty<File>();
	// The date that the image was taken
	private final ObjectProperty<LocalDateTime> dateTaken = new SimpleObjectProperty<>();
	// The NEON site closest to the image
	private final ObjectProperty<BoundedSite> siteTaken = new SimpleObjectProperty<>(null);
	private final ObjectProperty<Location> locationTaken = new SimpleObjectProperty<>(null);
	private final StringProperty droneMaker = new SimpleStringProperty(null);
	private final StringProperty cameraModel = new SimpleStringProperty(null);
	private final ObjectProperty<Vector3> speed = new SimpleObjectProperty<>(new Vector3());
	private final ObjectProperty<Vector3> rotation = new SimpleObjectProperty<>(new Vector3());

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
	public void readFileMetadataIntoImage()
	{
		try
		{
			final String UNSPECIFIED = "Unspecified";

			//Read the metadata off of the imagee
			Map<Tag, String> imageMetadataMap = MetadataUtils.readImageMetadata(this.getFile());

			this.dateTaken.setValue(LocalDateTime.parse(imageMetadataMap.getOrDefault(StandardTag.DATE_TIME_ORIGINAL, LocalDateTime.now().format(DATE_FORMAT_FOR_DISK)), DATE_FORMAT_FOR_DISK));
			this.locationTaken.setValue(new Location(
					Double.parseDouble(imageMetadataMap.getOrDefault(StandardTag.GPS_LATITUDE, "0")),
					Double.parseDouble(imageMetadataMap.getOrDefault(StandardTag.GPS_LONGITUDE, "0")),
					Double.parseDouble(imageMetadataMap.getOrDefault(StandardTag.GPS_ALTITUDE, "0"))));
			this.droneMaker.setValue(imageMetadataMap.getOrDefault(StandardTag.MAKE, UNSPECIFIED));
			this.cameraModel.setValue(imageMetadataMap.getOrDefault(MetadataUtils.CustomTags.CAMERA_MODEL_NAME, UNSPECIFIED));
			this.speed.setValue(new Vector3(
					Double.parseDouble(imageMetadataMap.getOrDefault(MetadataUtils.CustomTags.SPEED_X, "0")),
					Double.parseDouble(imageMetadataMap.getOrDefault(MetadataUtils.CustomTags.SPEED_Y, "0")),
					Double.parseDouble(imageMetadataMap.getOrDefault(MetadataUtils.CustomTags.SPEED_Z, "0"))));
			this.rotation.setValue(new Vector3(
					Double.parseDouble(imageMetadataMap.getOrDefault(MetadataUtils.CustomTags.ROLL, "0")),
					Double.parseDouble(imageMetadataMap.getOrDefault(MetadataUtils.CustomTags.PITCH, "0")),
					Double.parseDouble(imageMetadataMap.getOrDefault(MetadataUtils.CustomTags.YAW, "0"))));
		}
		catch (Exception e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Error reading image metadata for file " + this.getFile().getName() + "!\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Used to initialize icon bindings to their default
	 */
	public void initIconBindings()
	{
		// Bind the image property to a conditional expression.
		// The image is checked if the location is valid and the species present list is not empty
		Binding<Image> imageBinding = Bindings.createObjectBinding(() ->
		{
			if (this.siteTaken.getValue() != null)
				return NEON_IMAGE_ICON;
			return DEFAULT_IMAGE_ICON;
		}, this.siteTaken);
		selectedImage.bind(imageBinding);
	}

	@Override
	public ObjectProperty<Image> treeIconProperty()
	{
		return this.selectedImage;
	}

	public File getFile()
	{
		return this.imageFile.getValue();
	}

	public ObjectProperty<File> getFileProperty()
	{
		return this.imageFile;
	}

	public void setDateTaken(LocalDateTime date)
	{
		this.dateTaken.setValue(date);
	}

	public LocalDateTime getDateTaken()
	{
		return dateTaken.getValue();
	}

	public ObjectProperty<LocalDateTime> dateTakenProperty()
	{
		return dateTaken;
	}

	public void setLocationTaken(Location locationTaken)
	{
		this.locationTaken.setValue(locationTaken);
	}

	public Location getLocationTaken()
	{
		return locationTaken.getValue();
	}

	public ObjectProperty<Location> locationTakenProperty()
	{
		return locationTaken;
	}

	public void setDroneMaker(String droneMaker)
	{
		this.droneMaker.setValue(droneMaker);
	}

	public String getDroneMaker()
	{
		return droneMaker.getValue();
	}

	public StringProperty droneMakerProperty()
	{
		return this.droneMaker;
	}

	public void setCameraModel(String cameraModel)
	{
		this.cameraModel.setValue(cameraModel);
	}

	public String getCameraModel()
	{
		return cameraModel.getValue();
	}

	public StringProperty cameraModelProperty()
	{
		return this.cameraModel;
	}

	public void setSpeed(Vector3 speed)
	{
		this.speed.setValue(speed);
	}

	public Vector3 getSpeed()
	{
		return speed.getValue();
	}

	public ObjectProperty<Vector3> speedProperty()
	{
		return this.speed;
	}

	public void setRotation(Vector3 rotation)
	{
		this.rotation.setValue(rotation);
	}

	public Vector3 getRotation()
	{
		return rotation.getValue();
	}

	public ObjectProperty<Vector3> rotationProperty()
	{
		return rotation;
	}

	public void setSiteTaken(BoundedSite siteTaken)
	{
		this.siteTaken.setValue(siteTaken);
	}

	public BoundedSite getSiteTaken()
	{
		return siteTaken.getValue();
	}

	public ObjectProperty<BoundedSite> siteTakenProperty()
	{
		return siteTaken;
	}
}
