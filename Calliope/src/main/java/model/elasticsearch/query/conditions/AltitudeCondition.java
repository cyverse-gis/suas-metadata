package model.elasticsearch.query.conditions;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.elasticsearch.query.ElasticSearchQuery;
import model.elasticsearch.query.QueryCondition;
import model.settings.SettingsData;

/**
 * Data model used by the "Altitude filter" query condition
 */
public class AltitudeCondition extends QueryCondition
{
	// The altitude to compute on
	private DoubleProperty altitude = new SimpleDoubleProperty(0);
	// The units to interpret altitude as
	private ObjectProperty<SettingsData.DistanceUnits> units = new SimpleObjectProperty<>(SettingsData.DistanceUnits.Meters);
	// The comparison operator
	private ObjectProperty<AltitudeComparisonOperators> comparisonOperator = new SimpleObjectProperty<>(AltitudeComparisonOperators.Equal);

	// A list of possible comparison operators to filter
	private ObservableList<AltitudeComparisonOperators> operatorList = FXCollections.observableArrayList(AltitudeComparisonOperators.values());
	// A list of possible units to filter
	private ObservableList<SettingsData.DistanceUnits> unitList = FXCollections.observableArrayList(SettingsData.DistanceUnits.values());

	/**
	 * This query condition ensures only selected years are queried for
	 *
	 * @param query The current state of the query before the appending
	 */
	@Override
	public void appendConditionToQuery(ElasticSearchQuery query)
	{
		if (this.comparisonOperator.getValue() != null)
		{
			Double distanceInMeters = this.units.getValue().formatToMeters(this.altitude.getValue());
			query.addAltitudeCondition(distanceInMeters, this.comparisonOperator.getValue());
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
		return "AltitudeCondition.fxml";
	}

	/**
	 * The altitude property as an integer
	 *
	 * @return Altitude to query
	 */
	public DoubleProperty altitudeProperty()
	{
		return this.altitude;
	}

	/**
	 * The units used by altitude
	 *
	 * @return The units of altitude
	 */
	public ObjectProperty<SettingsData.DistanceUnits> unitsProperty()
	{
		return this.units;
	}

	/**
	 * The operator used to compare the current altitude and the one on cyverse
	 *
	 * @return The comparison operator
	 */
	public ObjectProperty<AltitudeComparisonOperators> comparisonOperatorProperty()
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
	public ObservableList<AltitudeComparisonOperators> getOperatorList()
	{
		return this.operatorList;
	}

	/**
	 * Enum of altitude comparison operators
	 */
	public enum AltitudeComparisonOperators
	{
		Equal("Equal To"),
		GreaterThan("Greater Than"),
		GreaterThanOrEqual("Greater Than or Equal To"),
		LessThan("Less Than"),
		LessThanOrEqual("Less Than or Equal To");

		private String displayName;

		/**
		 * Constructor takes the name to display and an operator that is the query condition operator equivelant
		 *
		 * @param displayName The name to visually display
		 */
		AltitudeComparisonOperators(String displayName)
		{
			this.displayName = displayName;
		}

		/**
		 * Returns the display name as the altitude condition toString
		 *
		 * @return The display name
		 */
		@Override
		public String toString()
		{
			return this.displayName;
		}
	}
}
