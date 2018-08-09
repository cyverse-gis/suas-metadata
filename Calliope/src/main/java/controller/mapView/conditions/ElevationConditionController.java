package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import model.elasticsearch.query.NumericComparisonOperator;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.AltitudeCondition;
import model.elasticsearch.query.conditions.ElevationCondition;
import model.settings.SettingsData;
import org.apache.commons.lang.math.NumberUtils;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.fxmisc.easybind.EasyBind;

/**
 * Class used as a controller for the "Elevation filter" UI component
 */
public class ElevationConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	// The operator we are currently using to compare
	@FXML
	public ComboBox<NumericComparisonOperator> cbxOperators;
	// The current elevation to compare with
	@FXML
	public TextField txtElevation;
	// The unit of the elevation
	@FXML
	public ComboBox<SettingsData.DistanceUnits> cbxUnit;

	///
	/// FXML Bound Fields End
	///

	/**
	 * Initialize sets up validators to ensure that elevation is a valid number
	 */
	@FXML
	public void initialize()
	{
		ValidationSupport fieldValidator = new ValidationSupport();
		// The elevation must be a double!
		fieldValidator.registerValidator(this.txtElevation, true, Validator.createPredicateValidator(NumberUtils::isNumber, "Elevation must be a decimal value!"));
	}

	/**
	 * Initializes the controller with a data model to bind to
	 *
	 * @param queryCondition The data model which should be an elevation filter condition
	 */
	@Override
	public void initializeData(QueryCondition queryCondition)
	{
		if (queryCondition instanceof ElevationCondition)
		{
			ElevationCondition elevationCondition = (ElevationCondition) queryCondition;

			// Initialize our fields
			this.txtElevation.setText(elevationCondition.elevationProperty().getValue().toString());
			this.cbxOperators.setItems(elevationCondition.getOperatorList());
			this.cbxOperators.getSelectionModel().select(elevationCondition.comparisonOperatorProperty().getValue());
			this.cbxUnit.setItems(elevationCondition.getUnitList());
			this.cbxUnit.getSelectionModel().select(elevationCondition.unitsProperty().getValue());

			// Bind the new values to our model
			elevationCondition.elevationProperty().bind(EasyBind.map(this.txtElevation.textProperty(), elevation -> NumberUtils.toDouble(elevation, 0.0)));
			elevationCondition.comparisonOperatorProperty().bind(this.cbxOperators.getSelectionModel().selectedItemProperty());
			elevationCondition.unitsProperty().bind(this.cbxUnit.getSelectionModel().selectedItemProperty());
		}
	}
}