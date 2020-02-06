package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import model.elasticsearch.query.NumericComparisonOperator;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.AltitudeCondition;
import model.settings.SettingsData;
import org.apache.commons.lang3.math.NumberUtils;
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
	public ComboBox<NumericComparisonOperator> cbxOperators;
	@FXML
	public TextField txtAltitude;
	@FXML
	public ComboBox<SettingsData.DistanceUnits> cbxUnit;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Initialize sets up validators to ensure that altitude is a valid number
	 */
	@FXML
	public void initialize()
	{
		ValidationSupport fieldValidator = new ValidationSupport();
		// The altitude must be a double!
		fieldValidator.registerValidator(this.txtAltitude, true, Validator.createPredicateValidator(NumberUtils::isNumber, "Altitude must be a decimal value!"));
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
			altitudeCondition.altitudeProperty().bind(EasyBind.map(this.txtAltitude.textProperty(), altitude -> NumberUtils.toDouble(altitude, 0.0)));
			altitudeCondition.comparisonOperatorProperty().bind(this.cbxOperators.getSelectionModel().selectedItemProperty());
			altitudeCondition.unitsProperty().bind(this.cbxUnit.getSelectionModel().selectedItemProperty());
		}
	}
}
