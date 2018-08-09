package model.elasticsearch.query.conditions;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.elasticsearch.query.ElasticSearchQuery;
import model.elasticsearch.query.NumericComparisonOperator;
import model.elasticsearch.query.QueryCondition;
import model.settings.SettingsData;

/**
 * Data model used by the "Elevation filter" query condition
 */
public class ElevationCondition extends QueryCondition
{
	// The elevation to compute on
	private DoubleProperty elevation = new SimpleDoubleProperty(0);
	// The units to interpret elevation as
	private ObjectProperty<SettingsData.DistanceUnits> units = new SimpleObjectProperty<>(SettingsData.DistanceUnits.Meters);
	// The comparison operator
	private ObjectProperty<NumericComparisonOperator> comparisonOperator = new SimpleObjectProperty<>(NumericComparisonOperator.Equal);

	// A list of possible comparison operators to filter
	private ObservableList<NumericComparisonOperator> operatorList = FXCollections.observableArrayList(NumericComparisonOperator.values());
	// A list of possible units to filter
	private ObservableList<SettingsData.DistanceUnits> unitList = FXCollections.observableArrayList(SettingsData.DistanceUnits.values());

	/**
	 * This query condition ensures only elevations are queried for
	 *
	 * @param query The current state of the query before the appending
	 */
	@Override
	public void appendConditionToQuery(ElasticSearchQuery query)
	{
		if (this.comparisonOperator.getValue() != null)
		{
			// Make sure we convert the distance to meters
			Double distanceInMeters = this.units.getValue().formatToMeters(this.elevation.getValue());
			query.addElevationCondition(distanceInMeters, this.comparisonOperator.getValue());
		}
	}

	/**
	 * Returns the FXML document that can edit this data model
	 *
	 * @return An FXML UI document to edit this data model
	 */
	@Override
	public String getFXMLConditionEditor()
	{
		return "ElevationCondition.fxml";
	}

	/**
	 * The elevation property as an integer
	 *
	 * @return Elevation to query
	 */
	public DoubleProperty elevationProperty()
	{
		return this.elevation;
	}

	/**
	 * The units used by elevation
	 *
	 * @return The units of elevation
	 */
	public ObjectProperty<SettingsData.DistanceUnits> unitsProperty()
	{
		return this.units;
	}

	/**
	 * The operator used to compare the current elevation and the one on cyverse
	 *
	 * @return The comparison operator
	 */
	public ObjectProperty<NumericComparisonOperator> comparisonOperatorProperty()
	{
		return this.comparisonOperator;
	}

	/**
	 * Getter for all possible distance units
	 *
	 * @return A list of possible units
	 */
	public ObservableList<SettingsData.DistanceUnits> getUnitList()
	{
		return this.unitList;
	}

	/**
	 * Getter for all possible operators
	 *
	 * @return A list of possible operators
	 */
	public ObservableList<NumericComparisonOperator> getOperatorList()
	{
		return this.operatorList;
	}
}