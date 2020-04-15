package model.elasticsearch.query.conditions;

import controller.mapView.LayeredMap;
import fxmapcontrol.Location;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import model.elasticsearch.query.ElasticSearchQuery;
import model.elasticsearch.query.MapQueryCondition;

/**
 * Data model used by the "Viewport filter" query condition
 */
public class ViewportCondition extends MapQueryCondition
{
	/**
	 * Called when the current condition should be appended to the given query parameter
	 *
	 * @param query The query to append this condition to
	 */
	@Override
	public void appendConditionToQuery(ElasticSearchQuery query)
	{
		// Get the map so we can compute bounds
		LayeredMap map = this.getMap();

		// Compute the bounds of the map inside of the window, this is used to compute the extent to which we can see the map
		Bounds boundsInParent = map.getBoundsInParent();

		// Using the bounds we compute the maximum and minimum lat/long values which we will pass to our query
		// TODO: Modified this code to correct compiler errors, but I have no idea if the correct output is produced.
		// Location topLeft = map.getProjection().viewportPointToLocation(new Point2D(boundsInParent.getMinX(), boundsInParent.getMinY()));
		// Location bottomRight = map.getProjection().viewportPointToLocation(new Point2D(boundsInParent.getMaxX(), boundsInParent.getMaxY()));
		Location topLeft = map.getProjection().mapToLocation(new Point2D(boundsInParent.getMinX(), boundsInParent.getMinY()));
		Location bottomRight = map.getProjection().mapToLocation(new Point2D(boundsInParent.getMaxX(), boundsInParent.getMaxY()));

		// Set the query's viewport
		query.addBox(topLeft, bottomRight);
	}

	/**
	 * @return Returns the conditional FXML file
	 */
	@Override
	public String getFXMLConditionEditor()
	{
		return "ViewportCondition.fxml";
	}
}
