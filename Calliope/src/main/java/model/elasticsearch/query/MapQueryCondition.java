package model.elasticsearch.query;

import controller.mapView.LayeredMap;

/**
 * Extended version of the query condition that contains data about a map it's referencing
 */
public abstract class MapQueryCondition extends QueryCondition
{
	// The map to reference
	private LayeredMap map;

	/**
	 * @param map Setter for map
	 */
	public void setMap(LayeredMap map)
	{
		this.map = map;
	}

	/**
	 * @return Getter for map
	 */
	public LayeredMap getMap()
	{
		return this.map;
	}
}
