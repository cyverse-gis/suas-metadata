package model.location;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * A class representing a location (latitude, longitude, and elevation)
 * 
 * @author David Slovikosky
 */
public class Position
{
	// Properties of a position are the latitude, longitude, and elevation
	private final DoubleProperty latitude = new SimpleDoubleProperty();
	private final DoubleProperty longitude = new SimpleDoubleProperty();
	private final DoubleProperty elevation = new SimpleDoubleProperty();

	/**
	 * Position constructor
	 *
	 * @param lat
	 *            The latitude of the location
	 * @param lng
	 *            The longitude of the location
	 * @param elevation
	 *            The location elevation
	 */
	public Position(Double lat, Double lng, Double elevation)
	{
		this.latitude.setValue(lat);
		this.longitude.setValue(lng);
		this.elevation.setValue(elevation);
	}

	/**
	 * Default constructor sets values to invalid values
	 */
	public Position()
	{
		this.latitude.setValue(-1000);
		this.longitude.setValue(-1000);
		this.elevation.setValue(-20000);
	}

	/**
	 * @return True if latitude is between -85 and +85
	 */
	public Boolean latValid() { return this.latitude.getValue() <= 85.0 && this.latitude.getValue() >= -85.0; }

	/**
	 * @return True if longitude is between -180 and +180
	 */
	public Boolean lngValid() { return this.longitude.getValue() <= 180.0 && this.longitude.getValue() >= -180; }

	/**
	 * @return True if elevation is not the default -20000 value
	 */
	public Boolean elevationValid() { return this.elevation.getValue() != -20000; }

	/**
	 * @return True if the name, latitude, longitude, and elevation are valid
	 */
	public Boolean locationValid() { return latValid() && lngValid() && elevationValid(); }

	/**
	 * Set the latitude property
	 *
	 * @param lat The latitude property
	 */
	public void setLatitude(Double lat)
	{
		this.latitude.setValue(lat);
	}

	/**
	 * Get the latitude of the location
	 * 
	 * @return the latitude of the location
	 */
	public Double getLatitude()
	{
		return latitude.getValue();
	}

	/**
	 * Get the lat property
	 *
	 * @return The lat property
	 */
	public DoubleProperty latitudeProperty()
	{
		return latitude;
	}

	/**
	 * Set the longitude of the location
	 *
	 * @param lng The new longitude
	 */
	public void setLongitude(Double lng)
	{
		this.longitude.setValue(lng);
	}

	/**
	 * Get the longitude of the location
	 * 
	 * @return the longitude of the location
	 */
	public Double getLongitude()
	{
		return longitude.getValue();
	}

	/**
	 * Get the lng property
	 *
	 * @return The lng property
	 */
	public DoubleProperty longitudeProperty()
	{
		return longitude;
	}

	/**
	 * Set the elevation of the location
	 *
	 * @param elevation the new elevation in METERS
	 */
	public void setElevation(Double elevation)
	{
		this.elevation.setValue(elevation);
	}

	/**
	 * Get the elevation of the location
	 * 
	 * @return the elevation of the location
	 */
	public Double getElevation()
	{
		return elevation.getValue();
	}

	/**
	 * Get the elevation property
	 *
	 * @return The elevation property
	 */
	public DoubleProperty elevationProperty()
	{
		return elevation;
	}

	/**
	 * To string just returns the name of the location
	 */
	@Override
	public String toString()
	{
		return "\nLatitude: " + this.getLatitude() + "\nLongitude: " + this.getLongitude() + "\nElevation: " + this.getElevation();
	}
}
