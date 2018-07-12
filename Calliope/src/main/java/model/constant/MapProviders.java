package model.constant;

import fxmapcontrol.MapTileLayer;

public enum MapProviders
{
	OpenStreetMaps("Open Street Map", MapTileLayer.getOpenStreetMapLayer()),
	OpenTopoMap("Open Topo Map", new MapTileLayer("OpenTopoMap", "https://{c}.tile.opentopomap.org/{z}/{x}/{y}.png", 0, 17)),
	OpenMapSurferRoads("Open Map Surfer - Roads", new MapTileLayer("OpenMapSurferRoads", "https://korona.geog.uni-heidelberg.de/tiles/roads/x={x}&y={y}&z={z}", 0, 20)),
	EsriWorldStreetMap("Esri World Street Map", new MapTileLayer("EsriWorldStreetMap", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}", 0, 20)),
	EsriWorldTopoMap("Esri World Topo Map", new MapTileLayer("EsriWorldTopoMap", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}", 0, 20)),
	EsriWorldImagery("Esri World Imagery", new MapTileLayer("EsriWorldImagery", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}", 0, 20));

	private String name;
	private MapTileLayer mapTileProvider;

	MapProviders(String name, MapTileLayer mapTileProvider)
	{
		this.name = name;
		this.mapTileProvider = mapTileProvider;
	}

	@Override
	public String toString()
	{
		return this.name;
	}

	public MapTileLayer getMapTileProvider()
	{
		return this.mapTileProvider;
	}
}