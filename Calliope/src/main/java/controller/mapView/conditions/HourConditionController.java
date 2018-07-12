package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.StringConverter;
import model.elasticsearch.query.IQueryCondition;
import model.elasticsearch.query.conditions.HourCondition;
import org.apache.commons.lang.math.NumberUtils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Class used as a controller for the "Hour filter" UI component
 */
public class HourConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	@FXML
	public ListView<Integer> hourFilterListView;

	///
	/// FXML Bound Fields End
	///

	// A reference to the current data model stored by this controller
	private HourCondition hourCondition;

	/**
	 * Initialize does nothing for this specific filter
	 *
	 * @param location ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
	}

	/**
	 * Initializes the controller with a data model to bind to
	 *
	 * @param iQueryCondition The data model which should be a hour filter condition
	 */
	@Override
	public void initializeData(IQueryCondition iQueryCondition)
	{
		if (iQueryCondition instanceof HourCondition)
		{
			this.hourCondition = (HourCondition) iQueryCondition;
			// Set the items of the hour list view to the hours specified by the condition
			this.hourFilterListView.setItems(this.hourCondition.getHourList());
			this.hourFilterListView.setCellFactory(CheckBoxListCell.forListView(this.hourCondition::hourSelectedProperty, new StringConverter<Integer>()
			{
				@Override
				public String toString(Integer hourInteger)
				{
					return hourInteger.toString() + ":00 - " + hourInteger.toString() + ":59";
				}
				@Override
				public Integer fromString(String hourString)
				{
					return NumberUtils.toInt(hourString.split(":")[0], 0);
				}
			}));
			this.hourFilterListView.setEditable(true);
		}
	}

	/**
	 * Button used to select all hours for analysis use
	 *
	 * @param actionEvent consumed
	 */
	public void selectAllHours(ActionEvent actionEvent)
	{
		if (this.hourCondition != null)
			this.hourCondition.selectAll();
		actionEvent.consume();
	}

	/**
	 * Button used to select no hours to be part of the analysis
	 *
	 * @param actionEvent consumed
	 */
	public void selectNoHours(ActionEvent actionEvent)
	{
		if (this.hourCondition != null)
			this.hourCondition.selectNone();
		actionEvent.consume();
	}
}
