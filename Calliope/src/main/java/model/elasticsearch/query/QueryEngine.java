package model.elasticsearch.query;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.elasticsearch.query.conditions.*;

import java.util.function.Supplier;

public class QueryEngine
{
	private ObservableList<IQueryCondition> queryConditions = FXCollections.observableArrayList(condition -> new Observable[] {});
	private ObservableList<QueryFilters> QUERY_FILTERS = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(QueryFilters.values()));

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
		ALTITUDE_FILTER("Altitude filter", AltitudeCondition::new),
		YEAR_FILTER("Year Filter", YearCondition::new),
		MONTH_FILTER("Month Filter", MonthCondition::new),
		HOUR_FILTER("Hour Filter", HourCondition::new),
		NEON_SITE_FILTER("Neon Site Filter", NeonCondition::new),
		DAY_OF_WEEK_FILTER("Day of Week Filter", DayOfWeekCondition::new),
		START_TIME_FILTER("Start Date Filter", StartDateCondition::new),
		END_TIME_FILTER("End Date Filter", EndDateCondition::new),
		COLLECTION_FILTER("Collection Filter", CollectionCondition::new);

		private String displayName;
		private Supplier<IQueryCondition> instanceCreator;

		QueryFilters(String displayName, Supplier<IQueryCondition> instanceCreator)
		{
			this.displayName = displayName;
			this.instanceCreator = instanceCreator;
		}

		public IQueryCondition createInstance()
		{
			return instanceCreator.get();
		}

		@Override
		public String toString()
		{
			return this.displayName;
		}
	}
}
