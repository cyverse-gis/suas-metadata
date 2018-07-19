package model.elasticsearch.query.conditions;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.elasticsearch.query.ElasticSearchQuery;
import model.elasticsearch.query.QueryCondition;

/**
 * Data model used by the "Map Polygon filter" query condition
 */
public class MapPolygonCondition extends QueryCondition
{
	// A list of polygon points that listens to lat/long changes
	private ObservableList<ObservableLocation> points = FXCollections.observableArrayList(point -> new Observable[] { point.latitudeProperty(), point.longitudeProperty() });

	/**
	 * This query condition ensures only images taken inside the polygon
	 *
	 * @param query The current state of the query before the appending
	 */
	@Override
	public void appendConditionToQuery(ElasticSearchQuery query)
	{
		// We can only append this query condition if we have at least 3 polygon points
		if (this.points.size() >= 3)
			query.addPolygonCondition(points);
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
	 * @return Getter for point data
	 */
	public ObservableList<ObservableLocation> getPoints()
	{
		return this.points;
	}
}
