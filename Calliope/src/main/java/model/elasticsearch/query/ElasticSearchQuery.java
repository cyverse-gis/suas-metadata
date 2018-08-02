package model.elasticsearch.query;


import model.CalliopeData;
import model.constant.CalliopeMetadataFields;
import model.cyverse.ImageCollection;
import model.elasticsearch.query.conditions.AltitudeCondition;
import model.elasticsearch.query.conditions.ObservableLocation;
import model.site.Site;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoValidationMethod;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class representing a query to be sent to our ElasticSearch cluster
 */
public class ElasticSearchQuery
{
	// A list of collections to query for
	private Set<ImageCollection> collectionQuery = new HashSet<>();
	// A list of months to query for
	private Set<Integer> monthQuery = new HashSet<>();
	// A list of hours to query for
	private Set<Integer> hourQuery = new HashSet<>();
	// A list of days of week to query for
	private Set<Integer> dayOfWeekQuery = new HashSet<>();
	private Set<String> neonSiteQuery = new HashSet<>();

	// Query builder used to make queries that we will send out
	private final BoolQueryBuilder queryBuilder;

	/**
	 * Constructor initializes base query fields
	 */
	public ElasticSearchQuery()
	{
		this.queryBuilder = QueryBuilders.boolQuery();
	}

	/**
	 * Adds a given image collection to the query
	 *
	 * @param imageCollection The image collection to 'and' into the query
	 */
	public void addImageCollection(ImageCollection imageCollection)
	{
		this.collectionQuery.add(imageCollection);
	}

	/**
	 * Adds a given startYear to the query
	 *
	 * @param startYear The start year to 'and' into the query
	 * @param endYear The end year to 'and' into the query
	 */
	public void setStartAndEndYear(Integer startYear, Integer endYear)
	{
		queryBuilder.must().add(QueryBuilders.rangeQuery("imageMetadata.yearTaken").gte(startYear).lte(endYear));
	}

	/**
	 * Adds a given month to the query
	 *
	 * @param month The month to 'and' into the query
	 */
	public void addMonth(Integer month)
	{
		this.monthQuery.add(month);
	}

	/**
	 * Adds a given hour to the query
	 *
	 * @param hour The hour to 'and' into the query
	 */
	public void addHour(Integer hour)
	{
		this.hourQuery.add(hour);
	}

	/**
	 * Adds a given day of week to the query
	 *
	 * @param dayOfWeek The day of week to 'and' into the query
	 */
	public void addDayOfWeek(Integer dayOfWeek)
	{
		this.dayOfWeekQuery.add(dayOfWeek);
	}

	/**
	 * Adds a given site to the query
	 *
	 * @param site The site to 'and' into the query
	 */
	public void addSite(Site site)
	{
		this.neonSiteQuery.add(site.getCode());
	}

	/**
	 * Sets the start date that all images must be taken after
	 *
	 * @param startDate The start date
	 */
	public void setStartDate(LocalDateTime startDate)
	{
		this.queryBuilder.must().add(QueryBuilders.rangeQuery("imageMetadata.dateTaken").gte(startDate.atZone(ZoneId.systemDefault()).format(CalliopeMetadataFields.INDEX_DATE_TIME_FORMAT)));
	}

	/**
	 * Sets the end date that all images must be taken after
	 *
	 * @param endDate The end date
	 */
	public void setEndDate(LocalDateTime endDate)
	{
		this.queryBuilder.must().add(QueryBuilders.rangeQuery("imageMetadata.dateTaken").lte(endDate.atZone(ZoneId.systemDefault()).format(CalliopeMetadataFields.INDEX_DATE_TIME_FORMAT)));
	}

	/**
	 * Adds a condition on which altitude can be filtered with an operator argument
	 *
	 * @param altitude The altitude value to filter on
	 * @param operator The operator with which to test the given altitude, can be <, <=, >, >=, or =
	 */
	public void addAltitudeCondition(Double altitude, AltitudeCondition.AltitudeComparisonOperators operator)
	{
		switch (operator)
		{
			// Depending on the operator we pick a query to be used
			case Equal:
				this.queryBuilder.must().add(QueryBuilders.termQuery("imageMetadata.altitude", altitude));
				break;
			case GreaterThan:
				this.queryBuilder.must().add(QueryBuilders.rangeQuery("imageMetadata.altitude").gt(altitude));
				break;
			case GreaterThanOrEqual:
				this.queryBuilder.must().add(QueryBuilders.rangeQuery("imageMetadata.altitude").gte(altitude));
				break;
			case LessThan:
				this.queryBuilder.must().add(QueryBuilders.rangeQuery("imageMetadata.altitude").lt(altitude));
				break;
			case LessThanOrEqual:
				this.queryBuilder.must().add(QueryBuilders.rangeQuery("imageMetadata.altitude").lte(altitude));
				break;
			default:
				CalliopeData.getInstance().getErrorDisplay().printError("Got an impossible altitude condition");
				break;
		}
	}

	/**
	 * Adds a polygon given a list of positions that each image must be inside of
	 *
	 * @param observableLocations List of locations that can be used to make a polygon
	 */
	public void addPolygonCondition(List<ObservableLocation> observableLocations)
	{
		// Query on the image metadata's position. We need to map the ObservableLocations to GeoPoints
		this.queryBuilder.must().add(QueryBuilders.geoPolygonQuery("imageMetadata.position", observableLocations.stream().map(observableLocation -> new GeoPoint(observableLocation.getLatitude(), observableLocation.getLongitude())).collect(Collectors.toList())).setValidationMethod(GeoValidationMethod.IGNORE_MALFORMED));
	}

	/**
	 * Finalizes the ElasticSearch query and returns the builder
	 *
	 * @return The query builder ready to be executed
	 */
	public QueryBuilder build()
	{
		// Make sure that we have at least one collection we're looking for
		// Collections are IDd by UUID
		if (!collectionQuery.isEmpty())
			this.queryBuilder.must().add(QueryBuilders.termsQuery("collectionID", this.collectionQuery.stream().map(imageCollection -> imageCollection.getID().toString()).collect(Collectors.toList())));

		// Make sure that we have at least one month we're looking for
		// Months are IDd by ordinal value (1-12)
		if (!monthQuery.isEmpty())
			this.queryBuilder.must().add(QueryBuilders.termsQuery("imageMetadata.monthTaken", this.monthQuery));

		// Make sure that we have at least one hour we're looking for
		// Hours are IDd by ordinal value (1-24)
		if (!hourQuery.isEmpty())
			this.queryBuilder.must().add(QueryBuilders.termsQuery("imageMetadata.hourTaken", this.hourQuery));

		// Make sure that we have at least one day-of-week we're looking for
		// Days of week are IDd by ordinal value (1-7)
		if (!dayOfWeekQuery.isEmpty())
			this.queryBuilder.must().add(QueryBuilders.termsQuery("imageMetadata.dayOfWeekTaken", this.dayOfWeekQuery));

		// Make sure that we have at least one neon site we're looking for
		// Neon sites are IDd by code
		if (!neonSiteQuery.isEmpty())
			this.queryBuilder.must().add(QueryBuilders.termsQuery("imageMetadata.siteCode", this.neonSiteQuery));

		return this.queryBuilder;
	}
}
