package model.elevationAPI.jsonPOJOs;

/**
 * This is the raw data returned by the elevation API JSON
 */
public class ElevationQueryData
{
	// X is longitude
	private Double x = null;
	// Y is latitude
	private Double y = null;
	// Data_Source is where the elevation data was returned from
	private String Data_Source = null;
	// The only field we care about... the actual ground elevation
	private Double Elevation = null;
	// The units of the elevation, we always request meters
	private String Units = null;

	///
	/// Getters for the data
	///

	public Double getLatitude()
	{
		return this.y;
	}

	public Double getLongitude()
	{
		return this.x;
	}

	public String getDataSource()
	{
		return this.Data_Source;
	}

	public Double getElevation()
	{
		return this.Elevation;
	}

	public String getUnits()
	{
		return this.Units;
	}
}
