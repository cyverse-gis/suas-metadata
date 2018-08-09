package controller.mapView.conditions;

import controller.mapView.IConditionController;
import model.elasticsearch.query.QueryCondition;

/**
 * Class used as a controller for the "Elevation filter" UI component
 */
public class ViewportConditionController implements IConditionController
{
	/**
	 * Given a query condition this function initializes the controller's fields with the data object. In this case it does nothing
	 *
	 * @param queryCondition The query condition data model to bind to this controller
	 */
	@Override
	public void initializeData(QueryCondition queryCondition) {}
}
