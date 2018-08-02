package model.openElevation;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class ElevationDataEntry
{
	// The latitude of the request
	private DoubleProperty latitude = new SimpleDoubleProperty(0.0);
	// The elevation of the ground of the request
	private IntegerProperty elevation = new SimpleIntegerProperty(0);
	// The longitude of the request
	private DoubleProperty longitude = new SimpleDoubleProperty(0.0);

	///
	/// Getters only for latitude/longitude/elevation
	///

	public double getLatitude()
	{
		return this.latitude.getValue();
	}

	public DoubleProperty latitudeProperty()
	{
		return this.latitude;
	}

	public int getElevation()
	{
		return this.elevation.getValue();
	}

	public IntegerProperty elevationProperty()
	{
		return this.elevation;
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
