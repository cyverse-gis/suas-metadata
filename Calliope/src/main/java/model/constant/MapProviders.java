package model.constant;

import fxmapcontrol.MapTileLayer;

/**
 * Utility enum with a list of possible map tile providers
 */
public enum MapProviders
{
	OpenStreetMaps("Open Street Map", "https://www.openstreetmap.org/", MapTileLayer.getOpenStreetMapLayer()),
	OpenTopoMap("Open Topo Map", "https://opentopomap.org/about", new MapTileLayer("OpenTopoMap", "https://{c}.tile.opentopomap.org/{z}/{x}/{y}.png", 0, 17)),
	GoogleTerrain("Google Terrain", "https://google.com/maps/", new MapTileLayer("GoogleTerrain", "http://mt0.google.com/vt/lyrs=p&hl=en&x={x}&y={y}&z={z}", 0, 19)),
	GoogleSatelliteHybrid("Google Satellite Hybrid","https://google.com/maps/", new MapTileLayer("GoogleHybrid", "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}", 0, 19));
	//EsriWorldImagery("Esri World Imagery", "https://www.esri.com/en-us/home", new MapTileLayer("EsriWorldImagery", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}", 0, 19));

	// The user-friendly name of the provider
	private String name;
	// The link to the website crediting this map tile providers
	private String creditURL;
	// The JavaFX map tile layer node
	private MapTileLayer mapTileProvider;

	/**
	 * Constructor just initializes fields
	 *
	 * @param name The user-friendly name of the provider
	 * @param creditURL The URL to the credits for this map tile layer
	 * @param mapTileProvider The JavaFX map tile layer node
	 */
	MapProviders(String name, String creditURL, MapTileLayer mapTileProvider)
	{
		this.name = name;
		this.creditURL = creditURL;
		this.mapTileProvider = mapTileProvider;
	}

	/**
	 * @return Just returns the name of the provider
	 */
	@Override
	public String toString()
	{
		return this.name;
	}

	/**
	 * @return A URL crediting this map tile provider
	 */
	public String getCreditURL()
	{
		return this.creditURL;
	}

	/**
	 * @return Returns the map tile provider for use on the map
	 */
	public MapTileLayer getMapTileProvider()
	{
		return this.mapTileProvider;
	}
}
