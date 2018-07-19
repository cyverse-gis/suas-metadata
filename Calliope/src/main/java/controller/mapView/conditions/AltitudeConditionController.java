package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.AltitudeCondition;
import model.settings.SettingsData;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.fxmisc.easybind.EasyBind;

/**
 * Class used as a controller for the "Altitude filter" UI component
 */
public class AltitudeConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	@FXML
	public ComboBox<AltitudeCondition.AltitudeComparisonOperators> cbxOperators;
	@FXML
	public TextField txtAltitude;
	@FXML
	public ComboBox<SettingsData.DistanceUnits> cbxUnit;

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
		// The altitude must be a double!
		fieldValidator.registerValidator(this.txtAltitude, true, Validator.createPredicateValidator(this::validDouble, "Altitude must be a decimal value!"));
	}

	/**
	 * Initializes the controller with a data model to bind to
	 *
	 * @param queryCondition The data model which should be an altitude filter condition
	 */
	@Override
	public void initializeData(QueryCondition queryCondition)
	{
		if (queryCondition instanceof AltitudeCondition)
		{
			AltitudeCondition altitudeCondition = (AltitudeCondition) queryCondition;

			// Initialize our fields
			this.txtAltitude.setText(altitudeCondition.altitudeProperty().getValue().toString());
			this.cbxOperators.setItems(altitudeCondition.getOperatorList());
			this.cbxOperators.getSelectionModel().select(altitudeCondition.comparisonOperatorProperty().getValue());
			this.cbxUnit.setItems(altitudeCondition.getUnitList());
			this.cbxUnit.getSelectionModel().select(altitudeCondition.unitsProperty().getValue());

			// Bind the new values to our model
			altitudeCondition.altitudeProperty().bind(EasyBind.map(this.txtAltitude.textProperty(), altitude -> parseOrDefault(altitude, 0.0)));
			altitudeCondition.comparisonOperatorProperty().bind(this.cbxOperators.getSelectionModel().selectedItemProperty());
			altitudeCondition.unitsProperty().bind(this.cbxUnit.getSelectionModel().selectedItemProperty());
		}
	}

	/**
	 * Parses the string number into a double, or returns the default number if the parse fails
	 *
	 * @param number The number to parse as a string
	 * @param defaultNumber The default return value
	 * @return The string as a number or the default number if the parse fails
	 */
	private Double parseOrDefault(String number, Double defaultNumber)
	{
		if (this.validDouble(number))
			return Double.parseDouble(number);
		else
			return defaultNumber;
	}

	/**
	 * Tests if a string is a valid double
	 *
	 * @param number The number to test
	 * @return True if the number is a valid double, false otherwise
	 */
	private Boolean validDouble(String number)
	{
		try
		{
			Double.parseDouble(number);
			return true;
		}
		catch (NumberFormatException ignored)
		{
			return false;
		}
	}
}
