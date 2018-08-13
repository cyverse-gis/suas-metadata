package model.elasticsearch.query.conditions;

import controller.mapView.LayeredMap;
import controller.mapView.MapLayers;
import fxmapcontrol.Location;
import fxmapcontrol.MapPolygon;
import javafx.collections.ObservableList;
import model.elasticsearch.query.ElasticSearchQuery;
import model.elasticsearch.query.MapQueryCondition;

import java.util.Comparator;

/**
 * Data model used by the "Map Box filter" query condition
 */
public class MapPolygonCondition extends MapQueryCondition
{
	// The polygon to edit and add to the map
	private MapPolygon polygon = new MapPolygon();

	/**
	 * Initialize the polygon with a styleclass and allow clicking through the polygon
	 */
	public MapPolygonCondition()
	{
		polygon.getStyleClass().add("box-boundary");
		polygon.setMouseTransparent(true);
	}

	/**
	 * Setter for the map, also ensure we remove the old polygon from the map and add the new polygon to the map
	 *
	 * @param map The new map
	 */
	@Override
	public void setMap(LayeredMap map)
	{
		// If we previously had a map remove the polygon from the map
		if (this.getMap() != null)
			this.getMap().removeChild(polygon);
		// Set the new map
		super.setMap(map);
		// Add the polygon to the new map
		map.addChild(polygon, MapLayers.QUERY_BOUNDARY);
	}

	/**
	 * This query condition ensures only images taken inside the box
	 *
	 * @param query The current state of the query before the appending
	 */
	@Override
	public void appendConditionToQuery(ElasticSearchQuery query)
	{
		query.addPolygon(polygon.getLocations());
	}

	/**
	 * Returns the FXML document that can edit this data model
	 *
	 * @return An FXML UI document to edit this data model
	 */
	@Override
	public String getFXMLConditionEditor()
	{
		return "MapPolygonCondition.fxml";
	}

	/**
	 * Called to destroy any listeners that this condition may have added to other view objects. In this case we remove the polygon child
	 */
	@Override
	public void destroy()
	{
		this.getMap().removeChild(polygon);
	}

	/**
	 * @return Getter for map polygon data
	 */
	public MapPolygon getPolygon()
	{
		return this.polygon;
	}
}
