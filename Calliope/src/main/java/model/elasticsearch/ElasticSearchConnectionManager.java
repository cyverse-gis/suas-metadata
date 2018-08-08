package model.elasticsearch;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import model.CalliopeData;
import model.constant.CalliopeMetadataFields;
import model.cyverse.ImageCollection;
import model.dataSources.UploadedEntry;
import model.image.ImageDirectory;
import model.image.ImageEntry;
import model.image.Position;
import model.image.Vector3;
import model.settings.SensitiveConfigurationManager;
import model.settings.SettingsData;
import model.site.Boundary;
import model.site.Site;
import model.site.ltar.LTARSite;
import model.site.neon.NEONSite;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
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
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.ParsedSingleBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoGridAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.geogrid.ParsedGeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.avg.ParsedAvg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.locationtech.jts.geom.Coordinate;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
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

	// The name of the sites index
	private static final String INDEX_CALLIOPE_SITES = "sites";
	// The type for the Calliope sites index
	private static final String INDEX_CALLIOPE_SITES_TYPE = "_doc";
	// The number of shards to be used by the sites index, for development we just need 1
	private static final Integer INDEX_CALLIOPE_SITES_SHARD_COUNT = 1;
	// The number of replicas to be created by the sites index, for development we don't need any
	private static final Integer INDEX_CALLIOPE_SITES_REPLICA_COUNT = 0;

	// The type used to serialize a list of cloud uploads
	private static final Type UPLOADED_ENTRY_LIST_TYPE = new TypeToken<ArrayList<UploadedEntry>>()
	{
	}.getType();

	// Create a new elastic search client
	private RestHighLevelClient elasticSearchClient;

	// Create a new elastic search schema manager
	private ElasticSearchSchemaManager elasticSearchSchemaManager;

	/**
	 * Given a username and password, this method logs a cyverse user in
	 *
	 * @param username The username of the CyVerse account
	 * @param password The password of the CyVerse account
	 * @return True if the login was successful, false otherwise
	 */
	public Boolean login(String username, String password)
	{
		SensitiveConfigurationManager configurationManager = CalliopeData.getInstance().getSensitiveConfigurationManager();
		// If the configuration manager has valid settings, initialize ES connection
		if (configurationManager.isConfigurationValid())
		{
			// Setup the credentials provider
			Credentials credentials = new UsernamePasswordCredentials(username, password);
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY, credentials);

			/*
			// SSL
			try
			{
				KeyStore trustStore = KeyStore.getInstance("jks");
				try (InputStream inputStream = Files.newInputStream(new File("./test.jks").toPath()))
				{
					trustStore.load(inputStream, password.toCharArray());
				}
				SSLContextBuilder sslContextBuilder = SSLContextBuilder.create().loadTrustMaterial(trustStore, null);
				SSLContext build = sslContextBuilder.build();
			}
			catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException e)
			{
				e.printStackTrace();
			}
			*/

			// Establish a connection to the elastic search server
			this.elasticSearchClient = new RestHighLevelClient(RestClient
					.builder(new HttpHost(configurationManager.getElasticSearchHost(), configurationManager.getElasticSearchPort(), ELASTIC_SEARCH_SCHEME))
					.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)).setMaxRetryTimeoutMillis(120000)
					.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setSocketTimeout(120000).setConnectTimeout(120000)));

			// Test to see if the ElasticSearch index is up or not
			try
			{
				if (this.elasticSearchClient.ping())
				{
					this.elasticSearchSchemaManager = new ElasticSearchSchemaManager();
					return true;
				}
				else
				{
					CalliopeData.getInstance().getErrorDisplay().notify("Could not establish a connection to the ElasticSearch cluster, is it down?");
				}
			}
			catch (ElasticsearchStatusException e)
			{
				CalliopeData.getInstance().getErrorDisplay().notify("Error connecting to the ElasticSearch index, error was " + e.status().toString());
			}
			catch (IOException e)
			{
				CalliopeData.getInstance().getErrorDisplay().notify("Error establishing a connection to the ElasticSearch index, error was:\n" + ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
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
	 * Destroys and rebuilds entire sites index. All sites stored will be lost
	 */
	public void nukeAndRecreateSitesIndex()
	{
		try
		{
			this.createIndex(
					INDEX_CALLIOPE_SITES,
					INDEX_CALLIOPE_SITES_TYPE,
					this.elasticSearchSchemaManager.makeCalliopeSiteIndexMapping(INDEX_CALLIOPE_SITES_TYPE),
					INDEX_CALLIOPE_SITES_SHARD_COUNT,
					INDEX_CALLIOPE_SITES_REPLICA_COUNT,
					true);

			// Use a bulk insert
			BulkRequest bulkRequest = new BulkRequest();

			// Download the list of sites from all sources
			List<? extends Site> sites = CalliopeData.getInstance().getSiteManager().downloadSites();

			// Iterate over all sites. Any that have duplicate site names will receive a number at the end signifying that they are in fact different parts of the same site
			sites.stream().collect(Collectors.groupingBy(Site::getName)).forEach((name, siteList) ->
			{
				// If our aggregated sites list has more than 1 entry, we need to mark each entry with a unique ID
				if (siteList.size() > 1)
					for (int i = 0; i < siteList.size(); i++)
					{
						Site site = siteList.get(i);
						// Add the name and code to each site
						site.setName(site.getName() + "-" + (i + 1));
						site.setCode(site.getCode() + "-" + (i + 1));
					}
			});

			// Iterate over each of the sites
			for (Site site : sites)
			{
				XContentBuilder xContentBuilder = this.elasticSearchSchemaManager.makeCreateSite(site);
				// Create an index request, use our schema manager to ensure the proper fields are inserted
				IndexRequest indexRequest = new IndexRequest()
						.index(INDEX_CALLIOPE_SITES)
						.type(INDEX_CALLIOPE_SITES_TYPE)
						.source(xContentBuilder);
				bulkRequest.add(indexRequest);
			}

			bulkRequest.timeout(TimeValue.timeValueMinutes(15));

			// Store the response of the bulk insert
			BulkResponse bulkResponse = this.elasticSearchClient.bulk(bulkRequest);
			// Make sure it was OK, if not, print an error
			if (bulkResponse.status() != RestStatus.OK)
				CalliopeData.getInstance().getErrorDisplay().notify("Error executing bulk site insert! Status = " + bulkResponse.status());

			Integer failureCount = 0;
			// If there were any errors in the bulk insertion show them
			if (bulkResponse.hasFailures())
			{
				// A list of response responses
				BulkItemResponse[] responses = bulkResponse.getItems();
				for (int i = 0; i < responses.length; i++)
				{
					// Grab the response
					BulkItemResponse response = responses[i];
					// If it failed, print out the failure
					if (response.isFailed())
					{
						System.out.println("Site '" + sites.get(i).toString() + "' indexing failed: " + response.getFailureMessage());
						failureCount++;
					}
				}
			}
			// If we had any errors print out how many failed
			if (failureCount > 0)
			{
				CalliopeData.getInstance().getErrorDisplay().printError(failureCount.toString() + " out of " + sites.size() + " had invalid geometries!");
			}
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Error creating site index mapping and insertion. Error was:\n" + ExceptionUtils.getStackTrace(e));
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
	public List<Site> pullRemoteSites()
	{
		// A list of sites to return
		List<Site> toReturn = new ArrayList<>();

		// Because the site list could be potentially long, we use a scroll to ensure reading results in reasonable chunks
		Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1));
		// Create a search request, and populate the fields
		SearchRequest searchRequest = new SearchRequest();
		searchRequest
				.indices(INDEX_CALLIOPE_SITES)
				.types(INDEX_CALLIOPE_SITES_TYPE)
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
					// A sites map should have the following properties:
					if (sitesMap.containsKey("name") && sitesMap.containsKey("code") && sitesMap.containsKey("type") && sitesMap.containsKey("boundary") && sitesMap.containsKey("details"))
					{
						// Get each field as an object
						Object nameObj = sitesMap.get("name");
						Object codeObj = sitesMap.get("code");
						Object typeObj = sitesMap.get("type");
						Object boundaryMapObj = sitesMap.get("boundary");
						Object detailsObj = sitesMap.get("details");

						// Make sure each field has the proper type
						if (nameObj instanceof String && codeObj instanceof String && typeObj instanceof String && boundaryMapObj instanceof Map<?, ?> && detailsObj instanceof List<?>)
						{
							// Cast each object to the proper type
							String name = (String) nameObj;
							String code = (String) codeObj;
							String type = (String) typeObj;
							Map<String, Object> boundaryMap = (Map<String, Object>) boundaryMapObj;
							List<String> details = (List<String>) detailsObj;

							// The boundary map should have coordinates
							if (boundaryMap.containsKey("coordinates"))
							{
								// Grab the coordinates as an object
								Object boundaryRawObj = boundaryMap.get("coordinates");
								// Check if the raw boundary is a list
								if (boundaryRawObj instanceof List<?>)
								{
									// Cast the object to a list
									List<List<List<Double>>> boundaryRaw = (List<List<List<Double>>>) boundaryRawObj;

									// Compute the boundary from the raw boundary list
									Boundary boundary = this.rawToBoundary(boundaryRaw);

									// Test the type and parse the details accordingly
									if (type.equalsIgnoreCase("neon"))
									{
										// Make a NEON site and then parse the details, add it to be returned
										NEONSite site = new NEONSite(name, code, boundary);
										this.injectDetailsIntoNEONSite(site, details);
										toReturn.add(site);
									}
									else if (type.equalsIgnoreCase("ltar"))
									{
										// Make a LTAR site and then parse the details, add it to be returned
										LTARSite site = new LTARSite(name, code, boundary);
										this.injectDetailsIntoLTARSite(site, details);
										toReturn.add(site);
									}
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
				CalliopeData.getInstance().getErrorDisplay().notify("Could not clear the scroll when reading sites");
		}
		catch (IOException e)
		{
			// Something went wrong, so show an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error pulling remote neon sites, error was:\n" + ExceptionUtils.getStackTrace(e));
		}

		return toReturn;
	}

	/**
	 * Convert a list of lists of (longitude, latitude) lists to a boundary
	 *
	 * @param rawBoundary The raw boundary to be converted
	 * @return The boundary
	 */
	private Boundary rawToBoundary(List<List<List<Double>>> rawBoundary)
	{
		// The outer boundary is always the first list
		List<List<Double>> outerBoundary = rawBoundary.get(0);
		// The inner boundaries are any lists after the first
		List<List<List<Double>>> innerBoundary = rawBoundary.subList(1, rawBoundary.size());

		// Map the raw boundary to a list of geo-points. Do this for both outer and inner boundary
		return new Boundary(extractPoints(outerBoundary), innerBoundary.stream().map(this::extractPoints).collect(Collectors.toList()));
	}

	/**
	 * Given a list of raw points this method returns the same list in a structured format. The input should be a list of lists that have length 2, where the
	 * first entry is a longitude and second entry is a latitude
	 *
	 * @param rawPoints The raw point list to input
	 * @return A structured list using GeoPoints
	 */
	private List<GeoPoint> extractPoints(List<List<Double>> rawPoints)
	{
		return rawPoints.stream().map(pointPair -> new GeoPoint(pointPair.get(1), pointPair.get(0))).collect(Collectors.toList());
	}

	/**
	 * Attempts to parse the details and adds them to the NEON site
	 *
	 * @param neonSite The site to add details to
	 * @param details The details to add to the site
	 */
	private void injectDetailsIntoNEONSite(NEONSite neonSite, List<String> details)
	{
		// Iterate over each detail
		details.forEach(detail ->
		{
			// Grab the key and value which are separated by the first ':'
			String key = StringUtils.substringBefore(detail, ":");
			String value = StringUtils.substringAfter(detail, ":");
			// Based on the key, set the right field
			switch(key)
			{
				case "domainName":
					neonSite.setDomainName(value);
					break;
				case "domainCode":
					neonSite.setDomainCode(value);
					break;
				case "siteDescription":
					neonSite.setSiteDescription(value);
					break;
				case "siteType":
					neonSite.setSiteType(value);
					break;
				case "stateCode":
					neonSite.setStateCode(value);
					break;
				case "stateName":
					neonSite.setStateName(value);
					break;
				default:
					break;
			}
		});
	}

	/**
	 * Attempts to parse the details and adds them to the LTAR site
	 *
	 * @param ltarSite The site to add details to
	 * @param details The details to add to the site
	 */
	private void injectDetailsIntoLTARSite(LTARSite ltarSite, List<String> details)
	{
		// Iterate over each detail
		details.forEach(detail ->
		{
			// Grab the key and value which are separated by the first ':'
			String key = StringUtils.substringBefore(detail, ":");
			String value = StringUtils.substringAfter(detail, ":");
			// Based on the key, set the right field
			switch(key)
			{
				case "acronym":
					ltarSite.setAcronym(value);
					break;
				case "city":
					ltarSite.setCity(value);
					break;
				case "state":
					ltarSite.setState(value);
					break;
				default:
					break;
			}
		});
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
	 * Removes a collection from the index without removing any source images
	 *
	 * @param imageCollection The collection to remove
	 */
	public void removeCollection(ImageCollection imageCollection)
	{
		try
		{
			// Delete by query isn't supported, so we do it ourselves

			// Because the metadata index is long, we use a scroll to ensure reading results in reasonable chunks
			Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1));
			// Create a search request, and populate the fields
			SearchRequest searchRequest = new SearchRequest();
			searchRequest
					.indices(INDEX_CALLIOPE_METADATA)
					.types(INDEX_CALLIOPE_METADATA_TYPE)
					.scroll(scroll)
					.source(new SearchSourceBuilder()
							// Fetch results 500 at a time, and use a query that matches collection ID
							.size(500)
							.fetchSource(false)
							.query(QueryBuilders.termsQuery("collectionID", imageCollection.getID().toString())));

			try
			{
				// Grab the search results
				SearchResponse searchResponse = this.elasticSearchClient.search(searchRequest);
				// Store the scroll id that was returned because we specified a scroll in the search request
				String scrollID = searchResponse.getScrollId();
				// Get a list of metadata fields (hits)
				SearchHit[] searchHits = searchResponse.getHits().getHits();

				// Iterate while there are metadata entries to be read
				while (searchHits != null && searchHits.length > 0)
				{
					// Create a bulk delete request
					BulkRequest bulkRequest = new BulkRequest();

					// Iterate over all current results
					for (SearchHit searchHit : searchHits)
					{
						// Grab the ID of our search result
						String id = searchHit.getId();

						// Create a new delete request and prepare it to delete this entry because it matched our query
						DeleteRequest documentDeleteRequest = new DeleteRequest();
						documentDeleteRequest
								.index(INDEX_CALLIOPE_METADATA)
								.type(INDEX_CALLIOPE_METADATA_TYPE)
								.id(id);

						// Add the delete request
						bulkRequest.add(documentDeleteRequest);
					}

					// Perform the bulk delete
					BulkResponse bulkResponse = this.elasticSearchClient.bulk(bulkRequest);
					if (bulkResponse.hasFailures())
						CalliopeData.getInstance().getErrorDisplay().printError("Error performing bulk delete, status is " + bulkResponse.status().toString());

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
					CalliopeData.getInstance().getErrorDisplay().notify("Could not clear the scroll when reading metadata");
			}
			catch (IOException e)
			{
				// Something went wrong, so show an error
				CalliopeData.getInstance().getErrorDisplay().notify("Error removing metadata entries for the collection '" + imageCollection.getName() + "', error was:\n" + ExceptionUtils.getStackTrace(e));
			}

			// Create a delete request to delete the collection
			DeleteRequest deleteRequest = new DeleteRequest();
			deleteRequest
					.index(INDEX_CALLIOPE_COLLECTIONS)
					.type(INDEX_CALLIOPE_COLLECTIONS_TYPE)
					.id(imageCollection.getID().toString());

			// Delete the collection
			DeleteResponse deleteResponse = this.elasticSearchClient.delete(deleteRequest);
			// Print an error if the delete fails
			if (deleteResponse.status() != RestStatus.OK)
				CalliopeData.getInstance().getErrorDisplay().notify("Error deleting collection '" + imageCollection.getName() + "', status was " + deleteResponse.status());
		}
		catch (IOException e)
		{
			// Print an error if the delete fails
			CalliopeData.getInstance().getErrorDisplay().notify("Error deleting collection '" + imageCollection.getName() + "'\n" + ExceptionUtils.getStackTrace(e));
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
	 * @param directory The directory containing all images awaiting upload
	 * @param uploadEntry The upload entry representing this upload, will be put into our collections index
	 * @param collectionID The ID of the collection that these images will be uploaded to
	 * @param absolutePathCreator A function that accepts an image file as input and returns the absolute path (on the storage medium) of the image file as output
	 */
	@SuppressWarnings("unchecked")
	public void indexImages(ImageDirectory directory, UploadedEntry uploadEntry, String collectionID, Function<ImageEntry, String> absolutePathCreator)
	{
		// List of images to be uploaded
		List<ImageEntry> imageEntries = directory.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry).map(imageContainer -> (ImageEntry) imageContainer).collect(Collectors.toList());

		try
		{
			// Create a bulk index request to update all these images at once
			BulkRequest bulkRequest = new BulkRequest();

			// Convert the images to a map format ready to be converted to JSON
			for (ImageEntry imageEntry : imageEntries)
			{
				// Our image to JSON map will return 2 items, one is the ID of the document and one is the JSON request
				XContentBuilder json = this.elasticSearchSchemaManager.imageToJSON(imageEntry, collectionID, absolutePathCreator.apply(imageEntry));
				IndexRequest request = new IndexRequest()
						.index(INDEX_CALLIOPE_METADATA)
						.type(INDEX_CALLIOPE_METADATA_TYPE)
						.source(json);
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
				put("uploadPath", uploadEntry.getUploadPath());
				put("storageMethod", uploadEntry.getStorageMethod());
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
	 * Given a list of images this function returns a parallel array of site codes of sites that each image belongs to
	 *
	 * @param imageEntries The list of images to detect sites in
	 * @return A list of site codes as a parallel array to the original image list with null if no site is at the location
	 */
	@SuppressWarnings("unchecked")
	public String[] detectSites(List<ImageEntry> imageEntries)
	{
		// A parallel array to return
		String[] toReturn = new String[imageEntries.size()];

		// Create a multi search (one per image)
		MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
		// Search once per image
		for (ImageEntry imageEntry : imageEntries)
		{
			// Grab the image to search for
			// Create a search request
			SearchRequest searchRequest = new SearchRequest();
			try
			{
				// Initialize the search request
				searchRequest
						.indices(INDEX_CALLIOPE_SITES)
						.types(INDEX_CALLIOPE_SITES_TYPE)
						.source(new SearchSourceBuilder()
								// We only care about site code
								.fetchSource(new String[]{"code"}, new String[]{"boundary", "details", "name", "site.siteDescription", "type"})
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
				for (int i = 0; i < responses.length; i++)
				{
					// Grab the response, and pull the hits
					MultiSearchResponse.Item response = responses[i];
					SearchHit[] hits = response.getResponse().getHits().getHits();
					// If we got 1 hit, we have the right site. If we do not have a hit, return null for this image
					if (hits.length == 1)
					{
						// Grab the raw hit map
						Map<String, Object> siteMap = hits[0].getSourceAsMap();

						// Make sure our site field has a site code field
						if (siteMap.containsKey("code"))
						{
							// Grab the site code field
							Object siteCodeObj = siteMap.get("code");
							// Make sure the site code field is a string
							if (siteCodeObj instanceof String)
							{
								// Store the site code field
								toReturn[i] = (String) siteCodeObj;
							}
						}
					}
					else
					{
						// No results = no site
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
			CalliopeData.getInstance().getErrorDisplay().notify("Error performing multisearch for site codes.\n" + ExceptionUtils.getStackTrace(e));
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
	 * @param numDocIDSPerBucket The number of document IDs we are supposed to retrieve per bucket. This takes time but yields better map analysis
	 * @return A list of buckets containing a center point and a list of images inside
	 */
	public List<GeoBucket> performGeoAggregation(Double topLeftLat, Double topLeftLong, Double bottomRightLat, Double bottomRightLong, Integer depth1To12, QueryBuilder query, Integer numDocIDSPerBucket)
	{
		// Create a list of buckets to return
		List<GeoBucket> toReturn = new ArrayList<>();

		try
		{
			// We use a sub-aggregation to take each result from the geo box query and put it into a bucket based on its proximity to other images
			// Here we also specify precision (how close two images need to be to be in a bucket)
			GeoGridAggregationBuilder geoHashAggregation =
			AggregationBuilders.geohashGrid("cells").field("imageMetadata.position").precision(depth1To12)
				// Now that images are in a bucket we average their lat and longs to create a "center" position ready to return to our user.
				.subAggregation(AggregationBuilders.avg("center_lat").script(new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, "doc['imageMetadata.position'].lat", Collections.emptyMap())))
				.subAggregation(AggregationBuilders.avg("center_lon").script(new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, "doc['imageMetadata.position'].lon", Collections.emptyMap())))
				// This will be a list of documents that are inside of the cell bucket
				.subAggregation(AggregationBuilders.terms("document_ids").field("_id").size(numDocIDSPerBucket));

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
					.subAggregation(geoHashAggregation);

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
									List<String> knownDocumentIDs = new ArrayList<>();
									Double centerLat = null;
									Double centerLong = null;
									// Latitude and longitude are fetched as sub-aggregations, so pull those here
									// We also pull off the unique IDs of all the documents which all should have their own bucket
									for (Aggregation cellAggregation : bucket.getAggregations())
									{
										// If it's a ParsedAvg aggregation it's either the lat or long aggregation, figure that out and update the corresponding value
										if (cellAggregation instanceof ParsedAvg)
										{
											if (cellAggregation.getName().equals("center_lat"))
												centerLat = ((ParsedAvg) cellAggregation).getValue();
											else if (cellAggregation.getName().equals("center_lon"))
												centerLong = ((ParsedAvg) cellAggregation).getValue();
										}
										// If it's a ParsedStringTerm aggregation we got a list of document IDs. Read the IDs and store them
										else if (cellAggregation instanceof ParsedStringTerms && cellAggregation.getName().equals("document_ids"))
										{
											ParsedStringTerms docIDTerms = (ParsedStringTerms) cellAggregation;
											docIDTerms.getBuckets().forEach(documentBucket -> knownDocumentIDs.add(documentBucket.getKeyAsString()));
										}
									}

									// If we received sub-aggregation data, we're good so return the bucket
									if (centerLat != null && centerLong != null)
										toReturn.add(new GeoBucket(centerLat, centerLong, documentsInBucket, knownDocumentIDs));
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
	 * Retrieves a list of site codes that are within a box defined by the two corners
	 *
	 * @param topLeftLat The top left corner's latitude
	 * @param topLeftLong The top left corner's longitude
	 * @param bottomRightLat The bottom right corner's latitude
	 * @param bottomRightLong The bottom right corner's longitude
	 * @return A list of site codes that are within this bounding box
	 */
	public List<String> grabSiteCodesWithin(Double topLeftLat, Double topLeftLong, Double bottomRightLat, Double bottomRightLong)
	{
		List<String> toReturn = new ArrayList<>();

		try
		{
			// Because the site list could be potentially long, we use a scroll to ensure reading results in reasonable chunks
			Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1));
			// Create a search request, and populate the fields
			SearchRequest searchRequest = new SearchRequest();
			searchRequest
					.indices(INDEX_CALLIOPE_SITES)
					.types(INDEX_CALLIOPE_SITES_TYPE)
					.scroll(scroll)
					.source(new SearchSourceBuilder()
							// Fetch results 100 at a time, and use a query that matches everything
							.size(100)
							.fetchSource(new FetchSourceContext(true, new String[] { "code" }, null))
							// We use a geo-intersection query that tests if our viewport intersects the site's boundary
							.query(QueryBuilders.geoIntersectionQuery("boundary", new EnvelopeBuilder(new Coordinate(topLeftLong, topLeftLat), new Coordinate(bottomRightLong, bottomRightLat)))));


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
					// We should only get 1 map value, the code, which we can add to our list
					if (sitesMap.containsKey("code") && sitesMap.get("code") instanceof String)
					{
						toReturn.add((String) sitesMap.get("code"));
					}
				}

				// Now that we've processed this wave of results, get the next 100 results
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
				CalliopeData.getInstance().getErrorDisplay().notify("Could not clear the scroll when reading sites");
		}
		catch (IOException e)
		{
			// Something went wrong, so show an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error pulling remote sites, error was:\n" + ExceptionUtils.getStackTrace(e));
		}

		return toReturn;
	}

	/**
	 * Given a geo-bucket we look up that bucket's documents and retrieve more interesting metadata about them which is returned in a list. This is used to
	 * view specifics about a "geo-aggregation" dot found on the map tab
	 *
	 * @param geoBucket The bucket to pull data from
	 * @return A list of image results that contain advanced metadata about simple lat/long points or "dots" found on the map tab
	 */
	@SuppressWarnings("unchecked")
	public List<QueryImageEntry> performCircleLookup(GeoBucket geoBucket)
	{
		// Create a list of results to return
		List<QueryImageEntry> toReturn = new ArrayList<>();

		// If the geo-bucket is not null and non-empty, we perform a multi-get for each document ID
		if (geoBucket != null && !geoBucket.getKnownDocumentIDs().isEmpty())
		{
			// A multi-get request that gets metadata about each document
			MultiGetRequest multiGetRequest = new MultiGetRequest();
			// We only want specific fields which reduces the bandwidth uses, list those here
			FetchSourceContext fieldsWeWant = new FetchSourceContext(true,
					new String[] { "storagePath", "collectionID", "imageMetadata.altitude", "imageMetadata.cameraModel", "imageMetadata.dateTaken", "imageMetadata.droneMaker", "imageMetadata.elevation", "imageMetadata.fileType", "imageMetadata.focalLength", "imageMetadata.height", "imageMetadata.width", "imageMetadata.position", "imageMetadata.rotation", "imageMetadata.speed", "imageMetadata.siteCode" },
					null);
			// Iterate over all document IDs and add one get request for each one
			geoBucket.getKnownDocumentIDs().forEach(documentID ->
					multiGetRequest.add(new MultiGetRequest.Item(INDEX_CALLIOPE_METADATA, INDEX_CALLIOPE_METADATA_TYPE, documentID).fetchSourceContext(fieldsWeWant)));

			// Perform the get
			try
			{
				MultiGetResponse multiGetItemResponse = this.elasticSearchClient.multiGet(multiGetRequest);
				// Iterate over all results
				for (MultiGetItemResponse itemResponse : multiGetItemResponse.getResponses())
				{
					// Make sure the result was successful
					if (!itemResponse.isFailed())
					{
						// Grab the JSON response as a hash map
						Map<String, Object> sourceAsMap = itemResponse.getResponse().getSourceAsMap();
						// Ensure the JSON contains 3 keys
						if (sourceAsMap.containsKey("collectionID") &&
							sourceAsMap.containsKey("storagePath") &&
							sourceAsMap.containsKey("imageMetadata"))
						{
							// The imageMetadata object should be a map
							Object metadataMapObj = sourceAsMap.get("imageMetadata");
							// Double check if it's a map
							if (metadataMapObj instanceof Map<?, ?>)
							{
								// Cast the object to a map
								Map<String, Object> metadataMap = (Map<String, Object>) metadataMapObj;
								// This new map should have specific fields, test that
								if (metadataMap.containsKey("altitude") &&
									metadataMap.containsKey("cameraModel") &&
									metadataMap.containsKey("dateTaken") &&
									metadataMap.containsKey("droneMaker") &&
									metadataMap.containsKey("elevation") &&
									metadataMap.containsKey("fileType") &&
									metadataMap.containsKey("focalLength") &&
									metadataMap.containsKey("height") &&
									metadataMap.containsKey("width") &&
									metadataMap.containsKey("position") &&
									metadataMap.containsKey("rotation") &&
									metadataMap.containsKey("speed") &&
									metadataMap.containsKey("siteCode"))
								{
									Object speedMapObj = metadataMap.get("speed");
									Object rotationMapObj = metadataMap.get("rotation");
									if (speedMapObj instanceof Map<?, ?> && rotationMapObj instanceof Map<?, ?>)
									{
										Map<String, Double> speedMap = (Map<String, Double>) speedMapObj;
										Map<String, Double> rotationMap = (Map<String, Double>) rotationMapObj;

										// Grab the collection ID
										String collectionID = sourceAsMap.get("collectionID").toString();
										// Add a new GeoImageResult to return. Convert relevant fields to a usable format
										QueryImageEntry imageEntry = new QueryImageEntry();
										imageEntry.setIrodsAbsolutePath(sourceAsMap.get("storagePath").toString());
										imageEntry.setImageCollection(CalliopeData.getInstance().getCollectionList().stream().filter(collection -> collection.getID().toString().equals(collectionID)).findFirst().orElse(null));
										imageEntry.setAltitude(NumberUtils.toDouble(metadataMap.get("altitude").toString(), Double.NaN));
										imageEntry.setCameraModel(metadataMap.get("cameraModel").toString());
										imageEntry.setDateTaken(ZonedDateTime.parse(metadataMap.get("dateTaken").toString(), CalliopeMetadataFields.INDEX_DATE_TIME_FORMAT).toLocalDateTime());
										imageEntry.setDroneMaker(metadataMap.get("droneMaker").toString());
										String[] longAndLat = metadataMap.get("position").toString().split(", ");
										if (longAndLat.length == 2)
											imageEntry.setLocationTaken(new Position(NumberUtils.toDouble(longAndLat[1], Double.NaN), NumberUtils.toDouble(longAndLat[0], Double.NaN), NumberUtils.toDouble(metadataMap.get("elevation").toString(), Double.NaN)));
										imageEntry.setFileType(metadataMap.get("fileType").toString());
										imageEntry.setFocalLength(NumberUtils.toDouble(metadataMap.get("focalLength").toString(), Double.NaN));
										imageEntry.setWidth(NumberUtils.toDouble(metadataMap.get("width").toString(), Double.NaN));
										imageEntry.setHeight(NumberUtils.toDouble(metadataMap.get("height").toString(), Double.NaN));
										imageEntry.setSiteTaken(CalliopeData.getInstance().getSiteManager().getSiteByCode(metadataMap.get("siteCode").toString()));
										imageEntry.setSpeed(new Vector3(NumberUtils.toDouble(speedMap.get("x").toString(), Double.NaN), NumberUtils.toDouble(speedMap.get("y").toString(), Double.NaN), NumberUtils.toDouble(speedMap.get("z").toString(), Double.NaN)));
										imageEntry.setRotation(new Vector3(NumberUtils.toDouble(rotationMap.get("roll").toString(), Double.NaN), NumberUtils.toDouble(rotationMap.get("pitch").toString(), Double.NaN), NumberUtils.toDouble(rotationMap.get("yaw").toString(), Double.NaN)));
										toReturn.add(imageEntry);
									}
								}
							}
						}
					}
				}
			}
			catch (IOException e)
			{
				// Something went wrong, so show an error
				CalliopeData.getInstance().getErrorDisplay().notify("Error performing multi-get document get, error was:\n" + ExceptionUtils.getStackTrace(e));
			}
		}

		return toReturn;
	}

	/**
	 * Given a query this method returns a list of image file paths that match the query
	 *
	 * @param currentQuery The query to apply and get metadata from
	 * @return A list of absolute iRODS paths to pull from the ES index
	 */
	public List<String> getImagePathsMatching(QueryBuilder currentQuery)
	{
		List<String> toReturn = new ArrayList<>();

		try
		{
			// Perform a search request to count the number of results
			SearchRequest countSearchRequest = new SearchRequest();
			countSearchRequest
					.indices(INDEX_CALLIOPE_METADATA)
					.types(INDEX_CALLIOPE_METADATA_TYPE)
					.source(new SearchSourceBuilder()
						// Use size==0 to count the number of documents matching the query
						.size(0)
						.query(currentQuery));

			// Perform the count search
			SearchResponse countSearchResponse = this.elasticSearchClient.search(countSearchRequest);
			// Store the number of hits
			Long totalHits = countSearchResponse.getHits().totalHits;
			// Every aggregation we fire off will have a max of 1000 results
			final int MAX_AGGS_PER_SEARCH = 1000;
			// Compute how many queries we need to perform to get all results
			Integer partitionsRequired = Math.toIntExact(totalHits / MAX_AGGS_PER_SEARCH + 1);

			// Iterate once partition
			for (Integer partitionNumber = 0; partitionNumber < partitionsRequired; partitionNumber++)
			{
				// Create a search request, and populate the fields
				SearchRequest searchRequest = new SearchRequest();
				searchRequest
					.indices(INDEX_CALLIOPE_METADATA)
					.types(INDEX_CALLIOPE_METADATA_TYPE)
					.source(new SearchSourceBuilder()
						.fetchSource(false)
						.query(currentQuery)
						// Aggregate on the storage path list so we get all unique paths. We do it in partitions to ensure we don't get more than 1000 results at once
						.aggregation(AggregationBuilders.terms("paths").field("storagePath").includeExclude(new IncludeExclude(partitionNumber, partitionsRequired)).size(MAX_AGGS_PER_SEARCH)));

				// Grab the search results
				SearchResponse searchResponse = this.elasticSearchClient.search(searchRequest);
				// Get a list of paths (hits)
				Aggregations aggregations = searchResponse.getAggregations();

				// Iterate over all aggregations, there should only be 1
				for (Aggregation aggregation : aggregations.asList())
					// The one aggregation should be a parsed string terms aggregation
					if (aggregation instanceof ParsedStringTerms)
						// There should be #buckets == #images. Grab each bucket's key (which is the path) and add it to be returned
						((ParsedStringTerms) aggregation).getBuckets().forEach(bucket ->  toReturn.add(bucket.getKeyAsString()));
			}
		}
		catch (IOException e)
		{
			// Something went wrong, so show an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error pulling remote image file paths, error was:\n" + ExceptionUtils.getStackTrace(e));
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
