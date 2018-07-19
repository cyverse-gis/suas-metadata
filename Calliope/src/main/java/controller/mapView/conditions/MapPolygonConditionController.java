package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import model.elasticsearch.query.IQueryCondition;
import model.elasticsearch.query.conditions.MapPolygonCondition;
import model.elasticsearch.query.conditions.ObservableLocation;
import model.util.FXMLLoaderUtils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller class for the map polygon condition containing a list of points that make up a polygon
 */
public class MapPolygonConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	// A list of points
	@FXML
	public ListView<ObservableLocation> lvwPoints;

	///
	/// FXML Bound Fields End
	///

	// Store the model condition
	private MapPolygonCondition mapPolygonCondition;

	/**
	 * Initialize just sets up our point list view
	 *
	 * @param location ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		// Use a custom cell factory for the polygon list view
		this.lvwPoints.setCellFactory(x -> FXMLLoaderUtils.loadFXML("mapView/conditions/PolygonPointListEntry.fxml").getController());
	}

	/**
	 * Initializes this condition with data
	 *
	 * @param iQueryCondition The iQueryCondition controller
	 */
	@Override
	public void initializeData(IQueryCondition iQueryCondition)
	{
		// Test if the condition is of the right type
		if (iQueryCondition instanceof MapPolygonCondition)
		{
			// Store the data
			this.mapPolygonCondition = (MapPolygonCondition) iQueryCondition;
			// Set the list view items to our model's items
			this.lvwPoints.setItems(mapPolygonCondition.getPoints());
		}
	}

	/**
	 * Adds a new point to the polygon
	 *
	 * @param actionEvent consumed
	 */
	public void addPoint(ActionEvent actionEvent)
	{
		this.mapPolygonCondition.getPoints().add(new ObservableLocation(0D, 0D));
		actionEvent.consume();
	}

	/**
	 * Removes the selected polygon point from the polygon if it exists
	 *
	 * @param actionEvent consumed
	 */
	public void removePoint(ActionEvent actionEvent)
	{
		ObservableLocation selectedItem = this.lvwPoints.getSelectionModel().getSelectedItem();
		if (selectedItem != null)
			this.mapPolygonCondition.getPoints().remove(selectedItem);
		actionEvent.consume();
	}
}
