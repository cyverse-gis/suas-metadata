package model.elasticsearch.query;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.elasticsearch.query.conditions.*;

import java.util.function.Supplier;

public class QueryEngine
{
	private ObservableList<QueryCondition> queryConditions = FXCollections.observableArrayList(condition -> new Observable[] {});
	private ObservableList<QueryFilters> QUERY_FILTERS = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(QueryFilters.values()));

	public ObservableList<QueryCondition> getQueryConditions()
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
		ELEVATION_FILTER("Elevation filter", ElevationCondition::new),
		YEAR_FILTER("Year Filter", YearCondition::new),
		MONTH_FILTER("Month Filter", MonthCondition::new),
		HOUR_FILTER("Hour Filter", HourCondition::new),
		SITE_FILTER("Site Filter", SiteCondition::new),
		DAY_OF_WEEK_FILTER("Day of Week Filter", DayOfWeekCondition::new),
		START_TIME_FILTER("Start Date Filter", StartDateCondition::new),
		END_TIME_FILTER("End Date Filter", EndDateCondition::new),
		COLLECTION_FILTER("Collection Filter", CollectionCondition::new),
		MAP_POLYGON_FILTER("Map Polygon Filter", MapPolygonCondition::new),
		FILE_TYPE_FILTER("File Type Filter", FileTypeCondition::new),
		VIEWPORT_CONDITION("Viewport Filter", ViewportCondition::new);

		private String displayName;
		private Supplier<QueryCondition> instanceCreator;

		QueryFilters(String displayName, Supplier<QueryCondition> instanceCreator)
		{
			this.displayName = displayName;
			this.instanceCreator = instanceCreator;
		}

		public QueryCondition createInstance()
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
