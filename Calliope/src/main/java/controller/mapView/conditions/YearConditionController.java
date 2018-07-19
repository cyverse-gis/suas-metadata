package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.YearCondition;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.fxmisc.easybind.EasyBind;

import java.time.LocalDateTime;

/**
 * Class used as a controller for the "Year filter" UI component
 */
public class YearConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	// The text field with the starting year
	@FXML
	public TextField txtStartYear;
	// The text field with the end year
	@FXML
	public TextField txtEndYear;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Initialize sets up validators to ensure that start and end year are valid numbers
	 */
	@FXML
	public void initialize()
	{
		ValidationSupport fieldValidator = new ValidationSupport();
		fieldValidator.registerValidator(this.txtStartYear, true, Validator.createPredicateValidator(this::validInteger, "Start year must be an integer!"));
		fieldValidator.registerValidator(this.txtEndYear,   true, Validator.createPredicateValidator(this::validInteger, "End year must be an integer!"));
	}

	/**
	 * Initializes the controller with a data model to bind to
	 *
	 * @param queryCondition The data model which should be a year filter condition
	 */
	@Override
	public void initializeData(QueryCondition queryCondition)
	{
		if (queryCondition instanceof YearCondition)
		{
			YearCondition yearCondition = (YearCondition) queryCondition;
			this.txtStartYear.setText(yearCondition.startYearProperty().getValue().toString());
			this.txtEndYear.setText(yearCondition.endYearProperty().getValue().toString());
			// Bind the year start and end properties to the text parsed to an integer
			yearCondition.startYearProperty().bind(EasyBind.map(this.txtStartYear.textProperty(), year -> parseOrDefault(year, LocalDateTime.MIN.getYear())));
			yearCondition.endYearProperty().bind(EasyBind.map(this.txtEndYear.textProperty(), year -> parseOrDefault(year, LocalDateTime.MAX.getYear())));
		}
	}

	/**
	 * Parses the string number into an integer, or returns the default number if the parse fails
	 *
	 * @param number The number to parse as a string
	 * @param defaultNumber The default return value
	 * @return The string as a number or the default number if the parse fails
	 */
	private Integer parseOrDefault(String number, Integer defaultNumber)
	{
		if (this.validInteger(number))
			return Integer.parseInt(number);
		else
			return defaultNumber;
	}

	/**
	 * Tests if a string is a valid integer
	 *
	 * @param number The number to test
	 * @return True if the number is a valid integer, false otherwise
	 */
	private Boolean validInteger(String number)
	{
		try
		{
			Integer.parseInt(number);
			return true;
		}
		catch (NumberFormatException ignored)
		{
			return false;
		}
	}
}
