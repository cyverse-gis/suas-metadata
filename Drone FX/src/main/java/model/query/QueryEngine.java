package model.query;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.query.conditions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class QueryEngine
{
	private ObservableList<IQueryCondition> queryConditions = FXCollections.observableArrayList(condition -> new Observable[] {});
	private ObservableList<QueryFilters> QUERY_FILTERS = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(QueryFilters.values()));

	public QueryEngine()
	{
	}

	public ObservableList<IQueryCondition> getQueryConditions()
	{
		return this.queryConditions;
	}

	public ObservableList<QueryFilters> getQueryFilters()
	{
		return QUERY_FILTERS;
	}

	public enum QueryFilters
	{
		SPECIES_FILTER("Species Filter", SpeciesFilterCondition::new),
		LOCATION_FILTER("Location Filter", LocationFilterCondition::new),
		ELEVATION_FILTER("Elevation filter", ElevationCondition::new),
		YEAR_FILTER("Year Filter", YearCondition::new),
		MONTH_FILTER("Month Filter", MonthCondition::new),
		HOUR_FILTER("Hour Filter", HourCondition::new),
		DAY_OF_WEEK_FILTER("Day of Week Filter", DayOfWeekCondition::new),
		START_TIME_FILTER("Start Date Filter", StartDateCondition::new),
		END_TIME_FILTER("End Date Filter", EndDateCondition::new),
		COLLECTION_FILTER("Collection Filter", CollectionCondition::new);

		private String displayName;
		private Function<UUID, IQueryCondition> instanceCreator;

		QueryFilters(String displayName, Function<UUID, IQueryCondition> instanceCreator)
		{
			this.displayName = displayName;
			this.instanceCreator = instanceCreator;
		}

		public IQueryCondition createInstance(UUID sessionID)
		{
			return instanceCreator.apply(sessionID);
		}

		@Override
		public String toString()
		{
			return this.displayName;
		}
	}
}
