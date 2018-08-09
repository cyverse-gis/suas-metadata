package controller.mapView;

/**
 * An enum with a set of map layers and their respective Z-layer
  */
public enum MapLayers
{
	TILE_PROVIDER(0),
	BORDER_POLYGON(1),
	CIRCLES(2),
	QUERY_BOUNDARY(3),
	QUERY_CORNER(4),
	SITE_PINS(5);

	// The z-layer
	private Integer zLayer;

	/**
	 * Constructor just sets the z-layer field
	 *
	 * @param zLayer The map z-layer
	 */
	MapLayers(Integer zLayer)
	{
		this.zLayer = zLayer;
	}

	/**
	 * @return The Z layer that this enum represents
	 */
	public Integer getZLayer()
	{
		return this.zLayer;
	}
}