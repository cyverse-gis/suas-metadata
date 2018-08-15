package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.fxml.FXML;
import jfxtras.scene.control.LocalDateTimePicker;
import jfxtras.scene.control.LocalDateTimeTextField;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.StartDateCondition;

/**
 * Class used as a controller for the "Start date filter" UI component
 */
public class StartDateConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	// The date picker that selects the start date cap
	@FXML
	public LocalDateTimeTextField txtDateTime;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Does nothing for the start date condition controller
	 */
	@FXML
	public void initialize()
	{
	}

	/**
	 * Initializes this controller with data
	 *
	 * @param startDateCondition The data model for this start date condition
	 */
	public void initializeData(QueryCondition startDateCondition)
	{
		if (startDateCondition instanceof StartDateCondition)
		{
			this.txtDateTime.localDateTimeProperty().bindBidirectional(((StartDateCondition) startDateCondition).startDateProperty());
		}
	}
}
