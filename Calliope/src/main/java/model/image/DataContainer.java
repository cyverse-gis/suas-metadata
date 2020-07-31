package model.image;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.scene.image.Image;
import library.HierarchyData;
import model.settings.MetadataCustomItem;
import model.site.Site;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A recursive datatype containing more image containers
 */
public abstract class DataContainer implements HierarchyData<DataContainer>
{
	// The icon to use for all images at the moment
	protected static final Image DEFAULT_IMAGE_ICON = new Image(ImageEntry.class.getResource("/images/importWindow/imageIcon.png").toString());

	// A property to wrap the currently selected image property. Must not be static!
	protected transient final ObjectProperty<Image> icon = new SimpleObjectProperty<>(DEFAULT_IMAGE_ICON);
	// The actual file 
	protected final ObjectProperty<File> imageFile = new SimpleObjectProperty<File>();
	// The date that the image was taken
	protected final ObjectProperty<LocalDateTime> dateTaken = new SimpleObjectProperty<>();
	// The NEON site closest to the image
	protected final ListProperty<Site> siteTaken = new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>()));
	// The lat/long/elevation of this image
	protected final ObjectProperty<Position> positionTaken = new SimpleObjectProperty<>(null);
	// The name of the drone maker company
	protected final StringProperty droneMaker = new SimpleStringProperty(null);
	// The name of the drone camera model
	protected final StringProperty cameraModel = new SimpleStringProperty(null);
	// The speed the drone was traveling when the image was taken
	protected final ObjectProperty<Vector3> speed = new SimpleObjectProperty<>(new Vector3());
	// The rotation the drone had when the image was taken
	protected final ObjectProperty<Vector3> rotation = new SimpleObjectProperty<>(new Vector3());
	// The altitude the image was taken at above the ground
	protected final DoubleProperty altitude = new SimpleDoubleProperty(-1);
	// The type of the image file
	protected final StringProperty fileType = new SimpleStringProperty(null);
	// The focal length that the image was taken with
	protected final DoubleProperty focalLength = new SimpleDoubleProperty(-1);
	// The width and height of the image
	protected final DoubleProperty width = new SimpleDoubleProperty(-1);
	protected final DoubleProperty height = new SimpleDoubleProperty(-1);

	// All metadata that exiftool was able to read
	protected final StringProperty allMetadata = new SimpleStringProperty(null);

	// The raw metadata entries without any modifications
	protected transient final List<MetadataCustomItem> rawMetadata = new ArrayList<>();

	// If the image entry's metadata is currently ready to be edited
	protected transient final SimpleBooleanProperty metadataEditable = new SimpleBooleanProperty(true);


	// The file that this container represents. May be a directory or file
	public abstract File getFile();

	// Sets the site taken of the given image container
	public abstract void setSiteTaken(Site site);

	/**
	 * To string just prints out the file name by default
	 * @return The file name
	 */
	@Override
	public String toString()
	{
		return this.getFile().getName();
	}

	/**
	 * Since this datatype is recursive, return an empty list by default. Override this to get other behavior
	 *
	 * @return A list of children which makes this datatype recursive
	 */
	@Override
	public ObservableList<DataContainer> getChildren()
	{
		return FXCollections.emptyObservableList();
	}

	public ObjectProperty<File> fileProperty()
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

	public void setPositionTaken(Position positionTaken)
	{
		this.positionTaken.setValue(positionTaken);
	}

	public Position getPositionTaken()
	{
		return positionTaken.getValue();
	}

	public ObjectProperty<Position> positionTakenProperty()
	{
		return positionTaken;
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

	public void setAltitude(Double altitude)
	{
		this.altitude.setValue(altitude);
	}

	public double getAltitude()
	{
		return this.altitude.getValue();
	}

	public DoubleProperty altitudeProperty()
	{
		return this.altitude;
	}

	public void setFileType(String fileType)
	{
		this.fileType.setValue(fileType);
	}

	public String getFileType()
	{
		return this.fileType.getValue();
	}

	public StringProperty fileTypeProperty()
	{
		return this.fileType;
	}

	public void setFocalLength(Double focalLength)
	{
		this.focalLength.setValue(focalLength);
	}

	public double getFocalLength()
	{
		return this.focalLength.getValue();
	}

	public DoubleProperty focalLengthProperty()
	{
		return this.focalLength;
	}

	public void setWidth(Double width)
	{
		this.width.setValue(width);
	}

	public double getWidth()
	{
		return this.width.getValue();
	}

	public DoubleProperty widthProperty()
	{
		return this.width;
	}

	public void setHeight(Double height)
	{
		this.height.setValue(height);
	}

	public double getHeight()
	{
		return this.height.getValue();
	}

	public DoubleProperty heightProperty()
	{
		return this.height;
	}

	public List<Site> getSiteTaken()
	{
		return siteTaken.get();
	}

	public ListProperty<Site> siteTakenProperty()
	{
		return siteTaken;
	}

	public List<MetadataCustomItem> getRawMetadata()
	{
		return rawMetadata;
	}

	public boolean isMetadataEditable()
	{
		return metadataEditable.getValue();
	}

	public BooleanProperty metadataEditableProperty()
	{
		return this.metadataEditable;
	}
}
