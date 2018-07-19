package model.elasticsearch.query.conditions;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Much like the 'location' class, this one is just observable
 */
public class ObservableLocation
{
	// Latitude and longitude properties (so they're observable)
	private DoubleProperty latitude = new SimpleDoubleProperty(0.0);
	private DoubleProperty longitude = new SimpleDoubleProperty(0.0);

	/**
	 * Constructor just initializes fields
	 *
	 * @param latitude The latitude of this location
	 * @param longitude The longitude of the location
	 */
	public ObservableLocation(Double latitude, Double longitude)
	{
		this.latitude.setValue(latitude);
		this.longitude.setValue(longitude);
	}

	///
	/// Setter/Getters
	///

	public void setLatitude(double latitude)
	{
		this.latitude.setValue(latitude);
	}

	public double getLatitude()
	{
		return this.latitude.getValue();
	}

	public DoubleProperty latitudeProperty()
	{
		return this.latitude;
	}

	public void setLongitude(double longitude)
	{
		this.longitude.setValue(longitude);
	}

	public double getLongitude()
	{
		return this.longitude.getValue();
	}

	public DoubleProperty longitudeProperty()
	{
		return this.longitude;
	}
}
