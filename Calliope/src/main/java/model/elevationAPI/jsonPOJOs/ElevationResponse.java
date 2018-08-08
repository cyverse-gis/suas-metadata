package model.elevationAPI.jsonPOJOs;

/**
 * Class used to deserialize a JSON response from elevation API into an object
 */
public class ElevationResponse
{
	private ElevationQueryObject USGS_Elevation_Point_Query_Service = null;

	/**
	 * @return The elevation data entry
	 */
	public ElevationQueryObject getResults()
	{
		return this.USGS_Elevation_Point_Query_Service;
	}
}
