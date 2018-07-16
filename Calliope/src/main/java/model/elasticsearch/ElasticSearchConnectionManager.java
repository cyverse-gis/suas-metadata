package model.elasticsearch;

import com.google.gson.reflect.TypeToken;
import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import javafx.application.Platform;
import model.CalliopeData;
import model.constant.CalliopeMetadataFields;
import model.cyverse.ImageCollection;
import model.dataSources.ImageDirectory;
import model.dataSources.ImageEntry;
import model.cyverse.UploadedEntry;
import model.neon.BoundedSite;
import model.neon.jsonPOJOs.Site;
import model.util.ErrorDisplay;
import model.util.SensitiveConfigurationManager;
import model.util.SettingsData;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.builders.PointBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.ParsedSingleBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.geogrid.ParsedGeoHashGrid;
import org.elasticsearch.search.aggregations.metrics.avg.ParsedAvg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class ElasticSearchConnectionManager
{
	// The scheme used to connect to the elastic search index
	private static final String ELASTIC_SEARCH_SCHEME = "http";

	// The name of the user's index
	private static final String INDEX_CALLIOPE_USERS = "users";
	// The type for the Calliope user's index
	private static final String INDEX_CALLIOPE_USERS_TYPE = "_doc";
	// The number of shards to be used by the users index, for development we just need 1
	private static final Integer INDEX_CALLIOPE_USERS_SHARD_COUNT = 1;
	// The number of replicas to be created by the users index, for development we don't need any
	private static final Integer INDEX_CALLIOPE_USERS_REPLICA_COUNT = 0;

	// The name of the metadata index
	private static final String INDEX_CALLIOPE_METADATA = "metadata";
	// The type for the Calliope metadata index
	private static final String INDEX_CALLIOPE_METADATA_TYPE = "_doc";
	// The number of shards to be used by the metadata index, for development we just need 1
	private static final Integer INDEX_CALLIOPE_METADATA_SHARD_COUNT = 1;
	// The number of replicas to be created by the metadata index, for development we don't need any
	private static final Integer INDEX_CALLIOPE_METADATA_REPLICA_COUNT = 0;

	// The name of the collections index
	private static final String INDEX_CALLIOPE_COLLECTIONS = "collections";
	// The type for the Calliope collections index
	private static final String INDEX_CALLIOPE_COLLECTIONS_TYPE = "_doc";
	// The number of shards to be used by the collections index, for development we just need 1
	private static final Integer INDEX_CALLIOPE_COLLECTIONS_SHARD_COUNT = 1;
	// The number of replicas to be created by the collections index, for development we don't need any
	private static final Integer INDEX_CALLIOPE_COLLECTIONS_REPLICA_COUNT = 0;

	// The name of the neonSite index
	private static final String INDEX_CALLIOPE_NEON_SITES = "neon_sites";
	// The type for the Calliope neonSite index
	private static final String INDEX_CALLIOPE_NEON_SITES_TYPE = "_doc";
	// The number of shards to be used by the neonSite index, for development we just need 1
	private static final Integer INDEX_CALLIOPE_NEON_SITES_SHARD_COUNT = 1;
	// The number of replicas to be created by the neonSite index, for development we don't need any
	private static final Integer INDEX_CALLIOPE_NEON_SITES_REPLICA_COUNT = 0;

	// The type used to serialize a list of cloud uploads
	private static final Type UPLOADED_ENTRY_LIST_TYPE = new TypeToken<ArrayList<UploadedEntry>>()
	{
	}.getType();

	// Create a new elastic search client
	private final RestHighLevelClient elasticSearchClient;

	// Create a new elastic search schema manager
	private final ElasticSearchSchemaManager elasticSearchSchemaManager;

	/**
	 * The constructor initializes the elastic search
	 */
	public ElasticSearchConnectionManager(SensitiveConfigurationManager configurationManager, ErrorDisplay errorDisplay)
	{
		// Establish a connection to the elastic search server
		this.elasticSearchClient = new RestHighLevelClient(RestClient.builder(new HttpHost(configurationManager.getElasticSearchHost(), configurationManager.getElasticSearchPort(), ELASTIC_SEARCH_SCHEME)));

		// Test to see if the ElasticSearch index is up or not
		try
		{
			if (!this.elasticSearchClient.ping())
				errorDisplay.notify("Could not establish a connection to the ElasticSearch cluster, is it down?");
		}
		catch (IOException e)
		{
			errorDisplay.notify("Could not establish a connection to the ElasticSearch cluster, is it down?\n" + ExceptionUtils.getStackTrace(e));
		}

		this.elasticSearchSchemaManager = new ElasticSearchSchemaManager();
	}

	/**
	 * Destroys and rebuilds the entire user's index. All user data will be lost!
	 */
	public void nukeAndRecreateUserIndex()
	{
		try
		{
			this.createIndex(
					INDEX_CALLIOPE_USERS,
					INDEX_CALLIOPE_USERS_TYPE,
					this.elasticSearchSchemaManager.makeCalliopeUsersIndexMapping(INDEX_CALLIOPE_USERS_TYPE),
					INDEX_CALLIOPE_USERS_SHARD_COUNT,
					INDEX_CALLIOPE_USERS_REPLICA_COUNT,
					true);
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Error creating collections index mapping. Error was:\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Destroys and rebuilds entire metadata index. All metadata stored will be lost
	 */
	public void nukeAndRecreateMetadataIndex()
	{
		try
		{
			this.createIndex(
					INDEX_CALLIOPE_METADATA,
					INDEX_CALLIOPE_METADATA_TYPE,
					this.elasticSearchSchemaManager.makeCalliopeMetadataIndexMapping(INDEX_CALLIOPE_METADATA_TYPE),
					INDEX_CALLIOPE_METADATA_SHARD_COUNT,
					INDEX_CALLIOPE_METADATA_REPLICA_COUNT,
					true);
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Error creating collections index mapping. Error was:\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Destroys and rebuilds entire collections index. All collections stored will be lost
	 */
	public void nukeAndRecreateCollectionsIndex()
	{
		try
		{
			this.createIndex(
					INDEX_CALLIOPE_COLLECTIONS,
					INDEX_CALLIOPE_COLLECTIONS_TYPE,
					this.elasticSearchSchemaManager.makeCalliopeCollectionsIndexMapping(INDEX_CALLIOPE_COLLECTIONS_TYPE),
					INDEX_CALLIOPE_COLLECTIONS_SHARD_COUNT,
					INDEX_CALLIOPE_COLLECTIONS_REPLICA_COUNT,
					true);
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Error creating collections index mapping. Error was:\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Destroys and rebuilds entire neon sites index. All neon sites stored will be lost
	 */
	public void nukeAndRecreateNeonSitesIndex()
	{
		try
		{
			this.createIndex(
					INDEX_CALLIOPE_NEON_SITES,
					INDEX_CALLIOPE_NEON_SITES_TYPE,
					this.elasticSearchSchemaManager.makeCalliopeNeonSiteIndexMapping(INDEX_CALLIOPE_NEON_SITES_TYPE),
					INDEX_CALLIOPE_NEON_SITES_SHARD_COUNT,
					INDEX_CALLIOPE_NEON_SITES_REPLICA_COUNT,
					true);
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Error creating neon site index mapping. Error was:\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Given an elastic search client and an index, this method removes the index from the client
	 *
	 * @param index The index to remove
	 */
	private void deleteIndex(String index)
	{
		try
		{
			// Create a delete request to remove the Calliope Users index and execute it
			DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);
			this.elasticSearchClient.indices().delete(deleteIndexRequest);
		}
		catch (IOException e)
		{
			// If the delete fa	ils just print out an error message
			CalliopeData.getInstance().getErrorDisplay().notify("Error deleting '" + index + "' from the ElasticSearch index: \n" + ExceptionUtils.getStackTrace(e));
		}
		catch (ElasticsearchStatusException e)
		{
			// If the delete fails just print out an error message
			CalliopeData.getInstance().getErrorDisplay().notify("Delete failed, status = " + e.status());
		}
	}

	/**
	 * Creates an index given all necessary parameters
	 *
	 * @param index The name of the index
	 * @param type The type of the index
	 * @param mapping The mapping for the index
	 * @param shardCount The number of shards the index should use
	 * @param replicaCount The number of replicas the index should use
	 * @param deleteOriginalIfPresent Removes the current index if it is already present in the DB
	 */
	private void createIndex(String index, String type, XContentBuilder mapping, Integer shardCount, Integer replicaCount, Boolean deleteOriginalIfPresent)
	{
		try
		{
			// Perform a test if the index exists with a get index
			GetIndexRequest getIndexRequest = new GetIndexRequest();
			getIndexRequest
					.indices(index)
					.humanReadable(false)
					.includeDefaults(false)
					.local(false);

			// Boolean if it exists
			Boolean exists = this.elasticSearchClient.indices().exists(getIndexRequest);

			// Delete the original index if it exists and we want to delete the original
			if (deleteOriginalIfPresent && exists)
				this.deleteIndex(index);

			// If the delete original if present flag is checked, the index will be deleted. If not, then we check if it existed originally. If it did not create
			if (deleteOriginalIfPresent || !exists)
			{
				// Create a create index request
				CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
				// Make sure to set the number of shards and replicas
				createIndexRequest.settings(Settings.builder()
						.put("index.number_of_shards", shardCount)
						.put("index.number_of_replicas", replicaCount));
				// Add the type mapping which defines our schema
				createIndexRequest.mapping(type, mapping);
				// Execute the index request
				this.elasticSearchClient.indices().create(createIndexRequest);
			}
		}
		catch (IOException e)
		{
			// If the creation fails, print an error out
			CalliopeData.getInstance().getErrorDisplay().notify("Error creating '" + index + "' in the ElasticSearch index:\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Initializes the remote CALLIOPE directory which is more like the remote CALLIOPE index now. Indices are
	 * updated with default user settings if not present
	 *
	 * @param username The user to initialize
	 */
	public void initCalliopeRemoteDirectory(String username)
	{
		try
		{
			// Get the document corresponding to this user's username. By doing this we get the exact document which contains our user settings
			GetRequest getRequest = new GetRequest();
			getRequest
					.index(INDEX_CALLIOPE_USERS)
					.type(INDEX_CALLIOPE_USERS_TYPE)
					// Make sure the ID corresponds to our username
					.id(username)
					// Ignore source to speed up the fetch
					.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE);
			// Perform the GET request
			GetResponse getResponse = this.elasticSearchClient.get(getRequest);
			// If the user is not in the db... create an index entry for him
			if (!getResponse.isExists())
			{
				// Create an index request which we use to put data into the elastic search index
				IndexRequest indexRequest = new IndexRequest();
				indexRequest
						.index(INDEX_CALLIOPE_USERS)
						.type(INDEX_CALLIOPE_USERS_TYPE)
						// Make sure the ID is our username
						.id(username)
						// The source will be a new
						.source(this.elasticSearchSchemaManager.makeCreateUser(username));
				// Perform the index request
				this.elasticSearchClient.index(indexRequest);
			}
		}
		catch (IOException e)
		{
			// Print an error if anything went wrong
			CalliopeData.getInstance().getErrorDisplay().notify("Error initializing user '" + username + "' in the ElasticSearch index: \n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Fetches the user's settings from the ElasticSearch index
	 *
	 * @return The user's settings
	 *
	 * @param username The user to pull settings from
	 */
	public SettingsData pullRemoteSettings(String username)
	{
		// Pull the settings from the ElasticSearch cluster
		try
		{
			// Use a get request to pull the correct field
			GetRequest getRequest = new GetRequest();
			// Setup our get request, make sure to specify the user we want to query for and the source fields we want to return
			getRequest
					.index(INDEX_CALLIOPE_USERS)
					.type(INDEX_CALLIOPE_USERS_TYPE)
					.id(username)
					.fetchSourceContext(new FetchSourceContext(true));
			// Store the response
			GetResponse getResponse = this.elasticSearchClient.get(getRequest);
			// If we got a good response, grab it
			if (getResponse.isExists() && !getResponse.isSourceEmpty())
			{
				// Result comes back as a map, search the map for our field and return it
				Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
				Object settings = sourceAsMap.get("settings");
				// Settings should be a map, so test that
				if (settings instanceof Map<?, ?>)
				{
					// Convert this HashMap to JSON, and finally from JSON into the SettingsData object. Once this is done, return!
					String json = CalliopeData.getInstance().getGson().toJson(settings);
					if (json != null)
					{
						return CalliopeData.getInstance().getGson().fromJson(json, SettingsData.class);
					}
				}
			}
			else
			{
				// Bad response, print out an error message. User probably doesnt exist
				CalliopeData.getInstance().getErrorDisplay().notify("User not found on the DB. This should not be possible.");
			}
		}
		catch (IOException e)
		{
			// Error happened when executing a GET request. Print an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error pulling settings for the user '" + username + "' from the ElasticSearch index: \n" + ExceptionUtils.getStackTrace(e));
		}

		return null;
	}

	/**
	 * Fetches the global site list from the ElasticSearch index
	 *
	 * @return The user's sites
	 */
	@SuppressWarnings("unchecked")
	public List<BoundedSite> pullRemoteSites()
	{
		// A list of sites to return
		List<BoundedSite> toReturn = new ArrayList<>();

		// Because the site list could be potentially long, we use a scroll to ensure reading results in reasonable chunks
		Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1));
		// Create a search request, and populate the fields
		SearchRequest searchRequest = new SearchRequest();
		searchRequest
				.indices(INDEX_CALLIOPE_NEON_SITES)
				.types(INDEX_CALLIOPE_NEON_SITES_TYPE)
				.scroll(scroll)
				.source(new SearchSourceBuilder()
						// Fetch results 10 at a time, and use a query that matches everything
						.size(10)
						.fetchSource(true)
						.query(QueryBuilders.matchAllQuery()));

		try
		{
			// Grab the search results
			SearchResponse searchResponse = this.elasticSearchClient.search(searchRequest);
			// Store the scroll id that was returned because we specified a scroll in the search request
			String scrollID = searchResponse.getScrollId();
			// Get a list of sites (hits)
			SearchHit[] searchHits = searchResponse.getHits().getHits();

			// Iterate while there are more collections to be read
			while (searchHits != null && searchHits.length > 0)
			{
				// Iterate over all current results
				for (SearchHit searchHit : searchHits)
				{
					// Grab the sites as a map object
					Map<String, Object> sitesMap = searchHit.getSourceAsMap();
					if (sitesMap.containsKey("site") && sitesMap.containsKey("boundary"))
					{
						// Convert the map to JSON, and then into an ImageCollection object. It's a bit of a hack but it works well
						String siteJSON = CalliopeData.getInstance().getGson().toJson(sitesMap.get("site"));
						Site site = CalliopeData.getInstance().getGson().fromJson(siteJSON, Site.class);
						site.initFromJSON();

						// Test if the site has a boundary
						if (sitesMap.containsKey("boundary"))
						{
							// Grab the boundary map
							Object boundaryMap = sitesMap.get("boundary");
							// Make sure that it is indeed a map
							if (boundaryMap instanceof Map<?, ?>)
							{
								// Grab the coordinates list
								Object polygonObject = ((Map<?, ?>) boundaryMap).get("coordinates");
								// Make sure the polygon object is a list
								if (polygonObject instanceof List<?>)
								{
									// The object should be a list of lists of lists
									List<List<List<Double>>> polygonRaw = (List<List<List<Double>>>) polygonObject;

									// Create a new boundary polygon
									Polygon boundary = new Polygon();
									// Set the outer boundary to be the first polygon in the list
									boundary.setOuterBoundaryIs(this.rawToBoundary(polygonRaw.get(0)));
									// The rest of the polygons are inner boundaries, so map the remainder of the list to another list of boundary polygons
									boundary.setInnerBoundaryIs(polygonRaw.subList(1, polygonRaw.size()).stream().map(this::rawToBoundary).collect(Collectors.toList()));
									// Store the boundary
									toReturn.add(new BoundedSite(site, boundary));
								}
							}

						}
					}
				}

				// Now that we've processed this wave of results, get the next 10 results
				SearchScrollRequest scrollRequest = new SearchScrollRequest();
				// Setup the scroll request
				scrollRequest
						.scrollId(scrollID)
						.scroll(scroll);
				// Perform the scroll, yielding another set of results
				searchResponse = this.elasticSearchClient.searchScroll(scrollRequest);
				// Store the hits and the new scroll id
				scrollID = searchResponse.getScrollId();
				searchHits = searchResponse.getHits().getHits();
			}

			// Finish off the scroll request
			ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
			clearScrollRequest.addScrollId(scrollID);
			ClearScrollResponse clearScrollResponse = this.elasticSearchClient.clearScroll(clearScrollRequest);
			// If clearing the scroll request fails, show an error
			if (!clearScrollResponse.isSucceeded())
				CalliopeData.getInstance().getErrorDisplay().notify("Could not clear the scroll when reading neon sites");
		}
		catch (IOException e)
		{
			// Something went wrong, so show an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error pulling remote neon sites, error was:\n" + ExceptionUtils.getStackTrace(e));
		}

		return toReturn;
	}

	/**
	 * Convert a list of (longitude, latitude) lists to a linear ring boundary
	 *
	 * @param rawBoundary The raw boundary to be convereted
	 * @return The boundary
	 */
	private Boundary rawToBoundary(List<List<Double>> rawBoundary)
	{
		// Map the raw boundary to a list of coordinates, and then that coordinate list to a boundary
		return new Boundary().withLinearRing(new LinearRing().withCoordinates(rawBoundary.stream().map(latLongList -> new Coordinate(latLongList.get(0), latLongList.get(1))).collect(Collectors.toList())));
	}

	/**
	 * Fetches the user's collections from the ElasticSearch index
	 *
	 * @return The user's collections
	 */
	public List<ImageCollection> pullRemoteCollections()
	{
		// A list of collections to return
		List<ImageCollection> toReturn = new ArrayList<>();

		// Because the collection list could be potentially long, we use a scroll to ensure reading results in reasonable chunks
		Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1));
		// Create a search request, and populate the fields
		SearchRequest searchRequest = new SearchRequest();
		searchRequest
				.indices(INDEX_CALLIOPE_COLLECTIONS)
				.types(INDEX_CALLIOPE_COLLECTIONS_TYPE)
				.scroll(scroll)
				.source(new SearchSourceBuilder()
					// Fetch results 10 at a time, and use a query that matches everything
					.size(10)
					.fetchSource(true)
					.query(QueryBuilders.matchAllQuery()));

		try
		{
			// Grab the search results
			SearchResponse searchResponse = this.elasticSearchClient.search(searchRequest);
			// Store the scroll id that was returned because we specified a scroll in the search request
			String scrollID = searchResponse.getScrollId();
			// Get a list of collections (hits)
			SearchHit[] searchHits = searchResponse.getHits().getHits();

			// Iterate while there are more collections to be read
			while (searchHits != null && searchHits.length > 0)
			{
				// Iterate over all current results
				for (SearchHit searchHit : searchHits)
				{
					// Grab the collection as a map object
					Map<String, Object> collection = searchHit.getSourceAsMap();
					// Convert the map to JSON, and then into an ImageCollection object. It's a bit of a hack but it works well
					String collectionJSON = CalliopeData.getInstance().getGson().toJson(collection);
					toReturn.add(CalliopeData.getInstance().getGson().fromJson(collectionJSON, ImageCollection.class));
				}

				// Now that we've processed this wave of results, get the next 10 results
				SearchScrollRequest scrollRequest = new SearchScrollRequest();
				// Setup the scroll request
				scrollRequest
						.scrollId(scrollID)
						.scroll(scroll);
				// Perform the scroll, yielding another set of results
				searchResponse = this.elasticSearchClient.searchScroll(scrollRequest);
				// Store the hits and the new scroll id
				scrollID = searchResponse.getScrollId();
				searchHits = searchResponse.getHits().getHits();
			}

			// Finish off the scroll request
			ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
			clearScrollRequest.addScrollId(scrollID);
			ClearScrollResponse clearScrollResponse = this.elasticSearchClient.clearScroll(clearScrollRequest);
			// If clearing the scroll request fails, show an error
			if (!clearScrollResponse.isSucceeded())
				CalliopeData.getInstance().getErrorDisplay().notify("Could not clear the scroll when reading collections");
		}
		catch (IOException e)
		{
			// Something went wrong, so show an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error pulling remote collections, error was:\n" + ExceptionUtils.getStackTrace(e));
		}

		return toReturn;
	}

	/**
	 * Pushes local settings into the user's index for safe keeping
	 *
	 * @param settingsData The settings to be saved which will overwrite the old ones
	 */
	public void pushLocalSettings(SettingsData settingsData)
	{
		try
		{
			// Create the update request to update settings
			UpdateRequest updateRequest = new UpdateRequest();
			// Initialize the update request with data
			updateRequest
					.index(INDEX_CALLIOPE_USERS)
					.type(INDEX_CALLIOPE_USERS_TYPE)
					.id(CalliopeData.getInstance().getUsername())
					.doc(this.elasticSearchSchemaManager.makeSettingsUpdate(settingsData));

			// Perform the update and test the response
			UpdateResponse updateResponse = this.elasticSearchClient.update(updateRequest);

			// If the response is OK, continue, if not print an error
			if (updateResponse.status() != RestStatus.OK)
				CalliopeData.getInstance().getErrorDisplay().notify("Error syncing settings, error response was: " + updateResponse.status());
		}
		catch (IOException e)
		{
			// Print an error if the update fails
			CalliopeData.getInstance().getErrorDisplay().notify("Error updating settings for the user '" + CalliopeData.getInstance().getUsername() + "'\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Pushes a local collection into the collection index
	 *
	 * @param imageCollection The collection to save
	 */
	public void pushLocalCollection(ImageCollection imageCollection)
	{
		try
		{
			// Default index request which will automatically be used if document does not exist yet
			// This will just make a blank collection
			IndexRequest indexRequest = new IndexRequest();
			indexRequest
					.index(INDEX_CALLIOPE_COLLECTIONS)
					.type(INDEX_CALLIOPE_COLLECTIONS_TYPE)
					.id(imageCollection.getID().toString())
					.source(this.elasticSearchSchemaManager.makeCreateCollection(imageCollection));

			// Create the update request to update/create the collection
			UpdateRequest updateRequest = new UpdateRequest();
			// Initialize the update request with data
			updateRequest
					.index(INDEX_CALLIOPE_COLLECTIONS)
					.type(INDEX_CALLIOPE_COLLECTIONS_TYPE)
					.id(imageCollection.getID().toString())
					.doc(this.elasticSearchSchemaManager.makeCollectionUpdate(imageCollection))
					// Upsert means "if the collection does not exist, call this request"
					.upsert(indexRequest);

			// Perform the update and test the response
			UpdateResponse updateResponse = this.elasticSearchClient.update(updateRequest);

			// If the response is OK, continue, if not print an error
			if (updateResponse.status() != RestStatus.OK && updateResponse.status() != RestStatus.CREATED)
				CalliopeData.getInstance().getErrorDisplay().notify("Error saving collection '" + imageCollection.getName() + "', error response was: " + updateResponse.status());
		}
		catch (IOException e)
		{
			// Print an error if the update fails
			CalliopeData.getInstance().getErrorDisplay().notify("Error saving collection '" + imageCollection.getName() + "'\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Downloads the upload list for a given collection
	 *
	 * @param imageCollection The image collection which we want to retrieve uploads of
	 */
	public void retrieveAndInsertUploadListFor(ImageCollection imageCollection)
	{
		// Get a map of 'upload'->[List of uploads]
		Map<String, Object> uploadsForCollection = getUploadsForCollection(imageCollection.getID().toString());
		if (uploadsForCollection != null)
		{
			// Make sure our map does in fact have the uploads key
			if (uploadsForCollection.containsKey("uploads"))
			{
				// Grab the JSON representing the uploads list
				String uploadJSON = CalliopeData.getInstance().getGson().toJson(uploadsForCollection.get("uploads"));
				// Convert the JSON to a list of objects
				List<UploadedEntry> uploads = CalliopeData.getInstance().getGson().fromJson(uploadJSON, UPLOADED_ENTRY_LIST_TYPE);
				// Update our collection's uploads
				Platform.runLater(() -> imageCollection.getUploads().setAll(uploads));
			}
		}
	}

	/**
	 * Utility function used to get a list of uploads fro a given collection ID
	 *
	 * @param collectionID The ID of the collection we want to retrieve uploads for
	 * @return A map containing a list of uploads
	 */
	private Map<String, Object> getUploadsForCollection(String collectionID)
	{
		try
		{
			// Get the document corresponding to this imageCollection's ID
			GetRequest getRequest = new GetRequest();
			getRequest
					.index(INDEX_CALLIOPE_COLLECTIONS)
					.type(INDEX_CALLIOPE_COLLECTIONS_TYPE)
					// Make sure the ID corresponds to the imageCollection ID
					.id(collectionID)
					// Only fetch the uploads part of the document
					.fetchSourceContext(new FetchSourceContext(true, new String[] { "uploads" }, new String[] { "name", "organization", "contactInfo", "description", "id", "permissions" }));
			// Perform the GET request
			GetResponse getResponse = this.elasticSearchClient.get(getRequest);
			// It should exist...
			if (getResponse.isExists() && !getResponse.isSourceEmpty())
			{
				// Return the response
				return getResponse.getSourceAsMap();
			}
		}
		catch (IOException e)
		{
			// If something went wrong, print out an error.
			CalliopeData.getInstance().getErrorDisplay().notify("Error retrieving uploads for image collection '" + collectionID + "', error was:\n" + ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	/**
	 * Given a path, a collection ID, and a directory of images, this function indexes the directory of images into the ElasticSearch
	 * index.
	 * @param basePath The base path all images will be placed to on the datastore. Often will look like /iplant/home/user/uploads/
	 * @param collectionID The ID of the collection that these images will be uploaded to
	 * @param directory The directory containing all images awaiting upload
	 * @param uploadEntry The upload entry representing this upload, will be put into our collections index
	 */
	@SuppressWarnings("unchecked")
	public void indexImages(String basePath, String collectionID, ImageDirectory directory, UploadedEntry uploadEntry)
	{
		// List of images to be uploaded
		List<ImageEntry> imageEntries = directory.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry).map(imageContainer -> (ImageEntry) imageContainer).collect(Collectors.toList());

		// Compute the absolute path of the image directory
		String localDirAbsolutePath = directory.getFile().getAbsolutePath();

		try
		{
			// Create a bulk index request to update all these images at once
			BulkRequest bulkRequest = new BulkRequest();

			// Convert the images to a map format ready to be converted to JSON
			for (ImageEntry imageEntry : imageEntries)
			{
				// Our image to JSON map will return 2 items, one is the ID of the document and one is the JSON request
				Tuple<String, XContentBuilder> idAndJSON = this.elasticSearchSchemaManager.imageToJSONMap(imageEntry, collectionID, basePath, localDirAbsolutePath);
				IndexRequest request = new IndexRequest()
						.index(INDEX_CALLIOPE_METADATA)
						.type(INDEX_CALLIOPE_METADATA_TYPE)
						.id(idAndJSON.v1())
						.source(idAndJSON.v2());
				bulkRequest.add(request);
			}

			// Execute the bulk insert
			BulkResponse bulkResponse = this.elasticSearchClient.bulk(bulkRequest);

			// Check if everything went OK, if not return an error
			if (bulkResponse.status() != RestStatus.OK)
				CalliopeData.getInstance().getErrorDisplay().notify("Error bulk inserting metadata, error response was: " + bulkResponse.status());

			// Now that we've updated our metadata index, update the collections uploads field

			// Update the uploads field
			UpdateRequest updateRequest = new UpdateRequest();

			// We do this update with a script, and it needs 1 argument. Create of map of that 1 argument now
			HashMap<String, Object> args = new HashMap<>();
			// We can't use XContentBuilder so just use hashmaps. We set all appropriate fields and insert it as the arguments parameter
			args.put("upload", new HashMap<String, Object>()
			{{
				put("imageCount", uploadEntry.getImageCount());
				put("uploadDate", uploadEntry.getUploadDate().atZone(ZoneId.systemDefault()).format(CalliopeMetadataFields.INDEX_DATE_TIME_FORMAT));
				put("uploadUser", uploadEntry.getUploadUser());
				put("uploadIRODSPath", uploadEntry.getUploadIRODSPath());
			}});
			updateRequest
				.index(INDEX_CALLIOPE_COLLECTIONS)
				.type(INDEX_CALLIOPE_COLLECTIONS_TYPE)
				.id(collectionID)
				// We use a script because we're updating nested fields. The script written out looks like:
				/*
				ctx._source.uploads.add(params.upload)
				 */
				.script(new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, "ctx._source.uploads.add(params.upload)", args));
			// Execute the update, and save the result
			UpdateResponse updateResponse = this.elasticSearchClient.update(updateRequest);
			// If the response was not OK, print an error
			if (updateResponse.status() != RestStatus.OK)
				CalliopeData.getInstance().getErrorDisplay().notify("Could not update the Collection's index with a new upload!");
		}
		catch (IOException e)
		{
			// If the update failed for some reason, print that error
			CalliopeData.getInstance().getErrorDisplay().notify("Could not insert the upload into the collection index!\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Clears and reloads the NEON site cache from the NEON api
	 */
	public void refreshNeonSiteCache()
	{
		List<BoundedSite> boundedSites = CalliopeData.getInstance().getNeonData().retrieveBoundedSites();
		// Clear the current index
		this.nukeAndRecreateNeonSitesIndex();
		try
		{
			// Use a bulk insert
			BulkRequest bulkRequest = new BulkRequest();

			// Iterate over each of the bounded sites
			for (BoundedSite boundedSite : boundedSites)
			{
				// Create an index request, use our schema manager to ensure the proper fields are inserted
				IndexRequest indexRequest = new IndexRequest()
						.index(INDEX_CALLIOPE_NEON_SITES)
						.type(INDEX_CALLIOPE_NEON_SITES_TYPE)
						.source(this.elasticSearchSchemaManager.makeCreateNEONSite(boundedSite));
				bulkRequest.add(indexRequest);
			}

			// Store the response of the bulk insert
			BulkResponse bulkResponse = this.elasticSearchClient.bulk(bulkRequest);
			// Make sure it was OK, if not, print an error
			if (bulkResponse.status() != RestStatus.OK)
				CalliopeData.getInstance().getErrorDisplay().notify("Error executing bulk NEON insert! Status = " + bulkResponse.status());
		}
		catch (IOException e)
		{
			// The insert failed, print an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error inserting updated NEON sites into the index!\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Given a list of images this function returns a parallel array of site codes of NEON sites that each image belongs to
	 *
	 * @param imageEntries The list of images to detect NEON sites in
	 * @return A list of NEON site codes as a parallel array to the original image list with null if no NEON site is at the location
	 */
	@SuppressWarnings("unchecked")
	public String[] detectNEONSites(List<ImageEntry> imageEntries)
	{
		// A parallel array to return
		String[] toReturn = new String[imageEntries.size()];

		// Create a multi search (one per image)
		MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
		// Search once per image
		for (Integer i = 0; i < imageEntries.size(); i++)
		{
			// Grab the image to search for
			ImageEntry imageEntry = imageEntries.get(i);
			// Create a search request
			SearchRequest searchRequest = new SearchRequest();
			try
			{
				// Initialize the search request
				searchRequest
						.indices(INDEX_CALLIOPE_NEON_SITES)
						.types(INDEX_CALLIOPE_NEON_SITES_TYPE)
						.source(new SearchSourceBuilder()
								// We only care about site code
								.fetchSource(new String[] { "site.siteCode" }, new String[] { "boundary", "site.domainCode", "site.domainName", "site.siteDescription", "site.siteLatitude", "site.siteLongitude", "site.siteName", "site.siteType", "site.stateCode", "site.stateName" })
								// We only care about a single result
								.size(1)
								// We want to search where the polygon intersects our image's location (as a point)
								.query(QueryBuilders.geoIntersectionQuery("boundary", new PointBuilder().coordinate(imageEntry.getLocationTaken().getLongitude(), imageEntry.getLocationTaken().getLatitude()))));
				// Store the search request
				multiSearchRequest.add(searchRequest);
			}
			catch (IOException e)
			{
				// Return an error if something went wrong creating the index request
				CalliopeData.getInstance().getErrorDisplay().notify("Error creating search request for the image " + imageEntry.getFile().getAbsolutePath() + "\n" + ExceptionUtils.getStackTrace(e));
			}
		}

		try
		{
			// Execute the search
			MultiSearchResponse multiSearchResponse = this.elasticSearchClient.multiSearch(multiSearchRequest);
			// Grab all responses
			MultiSearchResponse.Item[] responses = multiSearchResponse.getResponses();
			// We should get one response per image
			if (multiSearchResponse.getResponses().length == imageEntries.size())
			{
				// Iterate over all responses
				for (Integer i = 0; i < responses.length; i++)
				{
					// Grab the response, and pull the hits
					MultiSearchResponse.Item response = responses[i];
					SearchHit[] hits = response.getResponse().getHits().getHits();
					// If we got 1 hit, we have the right site. If we do not have a hit, return null for this image
					if (hits.length == 1)
					{
						// Grab the raw hit map
						Map<String, Object> siteMap = hits[0].getSourceAsMap();
						// It should have a site field
						if (siteMap.containsKey("site"))
						{
							// Grab the site field
							Object siteDetailsMapObj = siteMap.get("site");
							// The site field should be a map
							if (siteDetailsMapObj instanceof Map<?, ?>)
							{
								// Convert the site field to a map
								Map<String, Object> siteDetailsMap = (Map<String, Object>) siteDetailsMapObj;
								// Make sure our site field has a site code field
								if (siteDetailsMap.containsKey("siteCode"))
								{
									// Grab the site code field
									Object siteCodeObj = siteDetailsMap.get("siteCode");
									// Make sure the site code field is a string
									if (siteCodeObj instanceof String)
									{
										// Store the site code field
										toReturn[i] = (String) siteCodeObj;
									}
								}
							}
						}
					}
					else
					{
						// No results = no NEON site
						toReturn[i] = null;
					}
				}
			}
			else
			{
				// The query did not return the proper amount of responses, print an error
				CalliopeData.getInstance().getErrorDisplay().notify("Did not get enough responses from the multisearch, this should not be possible.");
			}
		}
		catch (IOException e)
		{
			// The query failed, print an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error performing multisearch for NEON site codes.\n" + ExceptionUtils.getStackTrace(e));
		}

		return toReturn;
	}

	/**
	 * Function that takes in a Geo-Box as input and a precision depth and returns all images in that box aggregated into buckets with the given depth
	 *
	 * @param topLeftLat The coordinate representing the top left latitude of the bounding box
	 * @param topLeftLong The coordinate representing the top left longitude of the bounding box
	 * @param bottomRightLat The coordinate representing the bottom right latitude of the bounding box
	 * @param bottomRightLong The coordinate representing the top bottom right longitude of the bounding box
	 * @param depth1To12 A depth value in the range of 1-12 that specifies how tightly aggregated buckets should be. 12 means
	 *                   buckets are less than a meter across, and 1 means buckets are hundreds of KM across. A larger depth
	 *                   requires more time to receive results
	 * @param query The actual query to filter images by before aggregating
	 * @return A list of buckets containing a center point and a list of images inside
	 */
	public List<GeoBucket> performGeoAggregation(Double topLeftLat, Double topLeftLong, Double bottomRightLat, Double bottomRightLong, Integer depth1To12, QueryBuilder query)
	{
		// Create a list of buckets to return
		List<GeoBucket> toReturn = new ArrayList<>();

		try
		{
			// The aggregation is the hard part of this task, so build it first
			FilterAggregationBuilder aggregationQuery =
					// First we filter by bounding box
					AggregationBuilders
							// Call the filter 'filtered_cells'
							.filter("filtered_cells",
									// User query builders to create our filter
									QueryBuilders
											// Our query is on the position field which must be in the box created by:
											.geoBoundingBoxQuery("imageMetadata.position")
											// The top left corner and the bottom right corner, specified here
											.setCorners(new GeoPoint(topLeftLat, topLeftLong), new GeoPoint(bottomRightLat, bottomRightLong)))
							// We use a sub-aggregation to take each result from the geo box query and put it into a bucket based on its proximity to other images
							// Here we also specify precision (how close two images need to be to be in a bucket)
							.subAggregation(AggregationBuilders.geohashGrid("cells").field("imageMetadata.position").precision(depth1To12)
									// Now that images are in a bucket we average their lat and longs to create a "center" position ready to return to our user.
									.subAggregation(AggregationBuilders.avg("center_lat").script(new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, "doc['imageMetadata.position'].lat", Collections.emptyMap())))
									.subAggregation(AggregationBuilders.avg("center_lon").script(new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, "doc['imageMetadata.position'].lon", Collections.emptyMap()))));

			// Create a search request, and populate the fields
			SearchRequest searchRequest = new SearchRequest();
			searchRequest
					.indices(INDEX_CALLIOPE_METADATA)
					.types(INDEX_CALLIOPE_METADATA_TYPE)
					.source(new SearchSourceBuilder()
							// Fetch no results, we're only interested into aggregation portion of the query
							.size(0)
							// Don't fetch anything unnecessary
							.fetchSource(false)
							// Our query will match all documents if no query was provided
							.query(query == null ? QueryBuilders.matchAllQuery() : query)
							// Add our complex aggregation now
							.aggregation(aggregationQuery));

			try
			{
				// Grab the search results
				SearchResponse searchResponse = this.elasticSearchClient.search(searchRequest);
				// Grab the aggregations from those search results
				List<Aggregation> aggregationHits = searchResponse.getAggregations().asList();
				// Go over the aggregations (there should be just one)
				for (Aggregation aggregation : aggregationHits)
				{
					// Make sure we got the right type of aggregation
					if (aggregation instanceof ParsedSingleBucketAggregation && aggregation.getName().equals("filtered_cells"))
					{
						// Grab the sub-aggregations of the by bounding box filter
						ParsedSingleBucketAggregation cellsInView = (ParsedSingleBucketAggregation) aggregation;
						// Iterate over all sub-aggregations
						for (Aggregation subAggregation : cellsInView.getAggregations())
						{
							// Each of these sub-aggregations should be a geo-hash-grid with buckets
							if (subAggregation instanceof ParsedGeoHashGrid && subAggregation.getName().equals("cells"))
							{
								// Grab the hash grid
								ParsedGeoHashGrid geoHashGrid = (ParsedGeoHashGrid) subAggregation;
								// Iterate over all buckets inside of the hash grid
								for (GeoHashGrid.Bucket bucket : geoHashGrid.getBuckets())
								{
									// The bucket will include 3 pieces of info, latitude, longitude, and the number of documents in the bucket
									Long documentsInBucket = bucket.getDocCount();
									Double centerLat = null;
									Double centerLong = null;
									// Latitude and longitude are fetched as sub-aggregations, so pull those here
									for (Aggregation centerAgg : bucket.getAggregations())
									{
										if (centerAgg instanceof ParsedAvg)
										{
											if (centerAgg.getName().equals("center_lat"))
												centerLat = ((ParsedAvg) centerAgg).getValue();
											else if (centerAgg.getName().equals("center_lon"))
												centerLong = ((ParsedAvg) centerAgg).getValue();
										}
									}

									// If we received sub-aggregation data, we're good so return the bucket
									if (centerLat != null && centerLong != null)
										toReturn.add(new GeoBucket(centerLat, centerLong, documentsInBucket));
								}
							}
						}
					}
				}
			}
			catch (IOException e)
			{
				// Something went wrong, so show an error
				CalliopeData.getInstance().getErrorDisplay().notify("Error performing geo-aggregation, error was:\n" + ExceptionUtils.getStackTrace(e));
			}
		}
		catch (IllegalArgumentException e)
		{
			// The user somehow managed to pass illegal values to the aggregation by moving the map into a strange position. Print an error but recover
			CalliopeData.getInstance().getErrorDisplay().notify("Invalid geo-aggregation, error was:\n" + ExceptionUtils.getStackTrace(e));
		}

		return toReturn;
	}

	/**
	 * Finalize method is called like a deconstructor and can be used to clean up any floating objects
	 *
	 * @throws Throwable If finalization fails for some reason
	 */
	@Override
	protected void finalize() throws Throwable
	{
		super.finalize();

		// Close the elastic search connection
		try
		{
			this.elasticSearchClient.close();
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Could not close ElasticSearch connection: \n" + ExceptionUtils.getStackTrace(e));
		}
	}
}
