package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.fxml.FXML;
import jfxtras.scene.control.LocalDateTimePicker;
import jfxtras.scene.control.LocalDateTimeTextField;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.EndDateCondition;

/**
 * Class used as a controller for the "End date filter" UI component
 */
public class EndDateConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	// The date picker that selects the end date cap
	@FXML
	public LocalDateTimeTextField txtDateTime;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Initializes this controller with data
	 *
	 * @param endDateCondition The data model for this end date condition
	 */
	@Override
	public void initializeData(QueryCondition endDateCondition)
	{
		if (endDateCondition instanceof EndDateCondition)
			// Bind the date to the end condition's end date property
			this.txtDateTime.localDateTimeProperty().bindBidirectional(((EndDateCondition) endDateCondition).endDateProperty());
	}
}
