package model.elasticsearch;

import javafx.beans.property.*;

import java.time.LocalDateTime;

/**
 * A utility class which contains simple metadata about an image. This class is created after a query to the ElasticSearch metadata index is fired off
 */
public class GeoImageResult
{
	// The name of the image
	private final ReadOnlyStringWrapper name = new ReadOnlyStringWrapper();
	// The collection name that this image belongs to
	private final ReadOnlyStringWrapper collectionName = new ReadOnlyStringWrapper();
	// The latitude that this image was taken at
	private final ReadOnlyDoubleWrapper altitude = new ReadOnlyDoubleWrapper();
	// The camera model this image was taken with
	private final ReadOnlyStringWrapper cameraModel = new ReadOnlyStringWrapper();
	// The date this image was taken
	private final ReadOnlyObjectWrapper<LocalDateTime> date = new ReadOnlyObjectWrapper<>();

	/**
	 * Constructor just initializes fields
	 *
	 * @param name The name of the image
	 * @param collectionName The collection name that this image belongs to
	 * @param altitude The latitude that this image was taken at
	 * @param cameraModel The camera model this image was taken with
	 * @param date The date this image was taken
	 */
	public GeoImageResult(String name, String collectionName, Double altitude, String cameraModel, LocalDateTime date)
	{
		this.name.setValue(name);
		this.collectionName.setValue(collectionName);
		this.altitude.setValue(altitude);
		this.cameraModel.setValue(cameraModel);
		this.date.setValue(date);
	}

	///
	/// Getters for all the properties. They're all read-only
	///

	public ReadOnlyStringProperty nameProperty()
	{
		return this.name.getReadOnlyProperty();
	}

	public ReadOnlyStringProperty collectionNameProperty()
	{
		return this.collectionName.getReadOnlyProperty();
	}

	public ReadOnlyDoubleProperty altitudeProperty()
	{
		return this.altitude.getReadOnlyProperty();
	}

	public ReadOnlyStringProperty cameraModelProperty()
	{
		return this.cameraModel.getReadOnlyProperty();
	}

	public ReadOnlyObjectProperty<LocalDateTime> dateProperty()
	{
		return this.date.getReadOnlyProperty();
	}
}
