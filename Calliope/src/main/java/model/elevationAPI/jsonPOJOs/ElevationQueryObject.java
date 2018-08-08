package model.elevationAPI.jsonPOJOs;

/**
 * This is the second level object that is returned by the elevation API, it just contains the query data
 */
public class ElevationQueryObject
{
	private ElevationQueryData Elevation_Query = null;

	/**
	 * @return The elevation data
	 */
	public ElevationQueryData getElevationQuery()
	{
		return this.Elevation_Query;
	}
}
