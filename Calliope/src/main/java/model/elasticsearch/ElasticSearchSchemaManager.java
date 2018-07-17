package model.elasticsearch;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import model.CalliopeData;
import model.constant.CalliopeMetadataFields;
import model.cyverse.ImageCollection;
import model.image.ImageEntry;
import model.neon.BoundedSite;
import model.settings.SettingsData;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.elasticsearch.common.xcontent.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class used to create JSON requests from model classes for managing schemas
 */
public class ElasticSearchSchemaManager
{
	/**
	 * Helper function which returns the JSON required to create the user's index mapping
	 *
	 * @return An XContentBuilder which can be used to create JSON in Java
	 */
	XContentBuilder makeCalliopeUsersIndexMapping(String indexType) throws IOException
	{
		// Well, it's the builder design pattern. RIP me
		return XContentFactory.jsonBuilder()
		.startObject()
			.startObject(indexType)
				.startObject("properties")
					.startObject("username")
						.field("type", "keyword")
					.endObject()
					.startObject("settings")
						.field("type", "object")
						.startObject("properties")
							.startObject("dateFormat")
								.field("type", "keyword")
							.endObject()
							.startObject("timeFormat")
								.field("type", "keyword")
							.endObject()
							.startObject("locationFormat")
								.field("type", "keyword")
							.endObject()
							.startObject("distanceUnits")
								.field("type", "keyword")
							.endObject()
							.startObject("popupDisplaySec")
								.field("type", "double")
							.endObject()
							.startObject("noPopups")
								.field("type", "boolean")
							.endObject()
						.endObject()
					.endObject()
				.endObject()
			.endObject()
		.endObject();
	}

	/**
	 * Helper function which returns the JSON required to create the metadata index mapping
	 *
	 * @return An XContentBuilder which can be used to create JSON in Java
	 */
	XContentBuilder makeCalliopeMetadataIndexMapping(String indexType) throws IOException
	{
		// Well, it's the builder design pattern. RIP me
		return XContentFactory.jsonBuilder()
		.startObject()
			.startObject(indexType)
				.startObject("properties")
					.startObject("storagePath")
						.field("type", "keyword")
					.endObject()
					.startObject("collectionID")
						.field("type", "keyword")
					.endObject()
					.startObject("imageMetadata")
						.field("type", "object")
						.startObject("properties")
							.startObject("dateTaken")
								.field("type", "date")
								.field("format", "date_time")
							.endObject()
							.startObject("yearTaken")
								.field("type", "integer")
							.endObject()
							.startObject("monthTaken")
								.field("type", "integer")
							.endObject()
							.startObject("hourTaken")
								.field("type", "integer")
							.endObject()
							.startObject("dayOfYearTaken")
								.field("type", "integer")
							.endObject()
							.startObject("dayOfWeekTaken")
								.field("type", "integer")
							.endObject()
							.startObject("neonSiteCode")
								.field("type", "keyword")
							.endObject()
							.startObject("position")
								.field("type", "geo_point")
							.endObject()
							.startObject("altitude")
								.field("type", "double")
							.endObject()
							.startObject("droneMaker")
								.field("type", "keyword")
							.endObject()
							.startObject("cameraModel")
								.field("type", "keyword")
							.endObject()
							.startObject("speed")
								.field("type", "object")
								.startObject("properties")
									.startObject("x")
										.field("type", "double")
									.endObject()
									.startObject("y")
										.field("type", "double")
									.endObject()
									.startObject("z")
										.field("type", "double")
									.endObject()
								.endObject()
							.endObject()
							.startObject("rotation")
								.field("type", "object")
								.startObject("properties")
									.startObject("roll")
										.field("type", "double")
									.endObject()
									.startObject("pitch")
										.field("type", "double")
									.endObject()
									.startObject("yaw")
										.field("type", "double")
									.endObject()
								.endObject()
							.endObject()
						.endObject()
					.endObject()
				.endObject()
			.endObject()
		.endObject();
	}

	/**
	 * Helper function which returns the JSON required to create the collections index mapping
	 *
	 * @return An XContentBuilder which can be used to create JSON in Java
	 */
	XContentBuilder makeCalliopeCollectionsIndexMapping(String indexType) throws IOException
	{
		// Well, it's the builder design pattern. RIP me
		return XContentFactory.jsonBuilder()
		.startObject()
			.startObject(indexType)
				.startObject("properties")
					.startObject("name")
						.field("type", "keyword")
					.endObject()
					.startObject("organization")
						.field("type", "keyword")
					.endObject()
					.startObject("contactInfo")
						.field("type", "keyword")
					.endObject()
					.startObject("description")
						.field("type", "text")
					.endObject()
					.startObject("id")
						.field("type", "keyword")
					.endObject()
					.startObject("permissions")
						.field("type", "nested")
						.startObject("properties")
							.startObject("username")
								.field("type", "keyword")
							.endObject()
							.startObject("read")
								.field("type", "boolean")
							.endObject()
							.startObject("upload")
								.field("type", "boolean")
							.endObject()
							.startObject("owner")
								.field("type", "boolean")
							.endObject()
						.endObject()
					.endObject()
					.startObject("uploads")
						.field("type", "nested")
						.startObject("properties")
							.startObject("uploadUser")
								.field("type", "keyword")
							.endObject()
							.startObject("uploadDate")
								.field("type", "date")
								.field("format", "date_time")
							.endObject()
							.startObject("imageCount")
								.field("type", "integer")
							.endObject()
							.startObject("uploadPath")
								.field("type", "keyword")
							.endObject()
							.startObject("storageMethod")
								.field("type", "keyword")
							.endObject()
						.endObject()
					.endObject()
				.endObject()
			.endObject()
		.endObject();
	}

	/**
	 * Helper function which returns the JSON required to create the neonBoundaries index mapping
	 *
	 * @return An XContentBuilder which can be used to create JSON in Java
	 */
	XContentBuilder makeCalliopeNeonSiteIndexMapping(String indexType) throws IOException
	{
		// Well, it's the builder design pattern. RIP me
		return XContentFactory.jsonBuilder()
		.startObject()
			.startObject(indexType)
				.startObject("properties")
					.startObject("site")
						.field("type", "object")
						.startObject("properties")
							.startObject("domainCode")
								.field("type", "keyword")
							.endObject()
							.startObject("domainName")
								.field("type", "text")
							.endObject()
							.startObject("siteCode")
								.field("type", "keyword")
							.endObject()
							.startObject("siteDescription")
								.field("type", "text")
							.endObject()
							.startObject("siteLatitude")
								.field("type", "double")
							.endObject()
							.startObject("siteLongitude")
								.field("type", "double")
							.endObject()
							.startObject("siteName")
								.field("type", "text")
							.endObject()
							.startObject("siteType")
								.field("type", "keyword")
							.endObject()
							.startObject("stateCode")
								.field("type", "keyword")
							.endObject()
							.startObject("stateName")
								.field("type", "keyword")
							.endObject()
						.endObject()
					.endObject()
					.startObject("boundary")
						.field("type", "geo_shape")
					.endObject()
				.endObject()
			.endObject()
		.endObject();
	}

	/**
	 * Given a username, this function returns the JSON representing a default user with that username
	 *
	 * @param username The username of the user to be added into an index
	 * @return A JSON blob containing all default values ready to setup a user's account
	 */
	XContentBuilder makeCreateUser(String username)
	{
		XContentBuilder builder = null;
		// Massive try-with-resources block used to read 3 default JSON files, one containing settings, one containing locations, and one
		// containing species.
		try (InputStreamReader inputStreamSettingsReader = new InputStreamReader(this.getClass().getResourceAsStream("/settings.json"));
			 BufferedReader settingsFileReader = new BufferedReader(inputStreamSettingsReader))
		{
			// Read the settings json file
			String settingsJSON = settingsFileReader.lines().collect(Collectors.joining("\n"));
			XContentParser settingsParser = XContentFactory.xContent(XContentType.JSON).createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, settingsJSON.getBytes());

			// Setup the JSON by using the default values found in the JSON files
			builder = XContentFactory.jsonBuilder()
			.startObject()
				.field("username", username)
				.field("settings")
				.copyCurrentStructure(settingsParser)
			.endObject();
		}
		catch (IOException e)
		{
			// Print an error if something went wrong internally
			CalliopeData.getInstance().getErrorDisplay().notify("Could not insert a new user into the index!\n" + ExceptionUtils.getStackTrace(e));
		}
		return builder;
	}

	/**
	 * Given a settings object, this function will create a JSON blob contain all settings info
	 *
	 * @param settingsData The data to be stored in the JSON blob
	 * @return A content builder ready to be exported as JSON
	 */
	XContentBuilder makeSettingsUpdate(SettingsData settingsData) throws IOException
	{
		// Convert the settings data to JSON. Store the JSON and convert it to a content factory object
		String settingsJSON = CalliopeData.getInstance().getGson().toJson(settingsData);
		// The field is called settings, and the value is the JSON we just produced
		return XContentFactory.jsonBuilder()
		.startObject()
			.field("settings")
			.copyCurrentStructure(XContentFactory.xContent(XContentType.JSON).createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, settingsJSON.getBytes()))
		.endObject();
	}

	/**
	 * Utility function used to create a JSON request body which creates a collection
	 *
	 * @param imageCollection The image collection to create the request for
	 * @return A JSON builder formatted to create a collection
	 * @throws IOException If the JSON is improperly formatted
	 */
	XContentBuilder makeCreateCollection(ImageCollection imageCollection) throws IOException
	{
		// Convert the collection to JSON
		String collectionJSON = CalliopeData.getInstance().getGson().toJson(imageCollection);

		// Read this JSON directly and return it. Simple as that
		return XContentFactory.jsonBuilder()
			.copyCurrentStructure(XContentFactory.xContent(XContentType.JSON).createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, collectionJSON.getBytes()));
	}

	/**
	 * Utility function used to create a JSON request body which updates a collection's settings
	 *
	 * @param imageCollection The collection to update
	 * @return A JSON blob formatted to update the collection
	 * @throws IOException If something in the JSON went wrong
	 */
	XContentBuilder makeCollectionUpdate(ImageCollection imageCollection) throws IOException
	{
		// Convert the collection's permission data to JSON. Store the JSON and convert it to a content factory object
		// We can't do the entire document at once because we don't want to overwrite uploads
		String permissionsJSON = CalliopeData.getInstance().getGson().toJson(imageCollection.getPermissions());
		return XContentFactory.jsonBuilder()
		.startObject()
			// Setup all the basic fields
			.field("contactInfo", imageCollection.getContactInfo())
			.field("description", imageCollection.getDescription())
			.field("id", imageCollection.getID().toString())
			.field("name", imageCollection.getName())
			.field("organization", imageCollection.getOrganization())
			// Permissions are pulled permission list
			.field("permissions")
			.copyCurrentStructure(XContentFactory.xContent(XContentType.JSON).createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, permissionsJSON.getBytes()))
		.endObject();
	}

	/**
	 * Utility function used to convert an image entry to its JSON representation
	 *
	 * @param imageEntry The image to convert to its JSON representation
	 * @param collectionID The ID of the collection that the image belongs to
	 * @param fileAbsolutePath The absolute path of the file on CyVerse
	 * @return A map of key->value pairs used later in creating JSON
	 */
	XContentBuilder imageToJSON(ImageEntry imageEntry, String collectionID, String fileAbsolutePath) throws IOException
	{
		// On windows paths have \ as a path separator vs unix /. Make sure that we always use /
		String fixedAbsolutePath = fileAbsolutePath.replace('\\', '/');

		// We return the JSON representing the image metadata
		return XContentFactory.jsonBuilder()
		.startObject()
			.field("storagePath", fixedAbsolutePath)
			.field("collectionID", collectionID)
			.startObject("imageMetadata")
				.field("dateTaken", imageEntry.getDateTaken().atZone(ZoneId.systemDefault()).format(CalliopeMetadataFields.INDEX_DATE_TIME_FORMAT))
				.field("yearTaken", imageEntry.getDateTaken().getYear())
				.field("monthTaken", imageEntry.getDateTaken().getMonthValue())
				.field("hourTaken", imageEntry.getDateTaken().getHour())
				.field("dayOfYearTaken", imageEntry.getDateTaken().getDayOfYear())
				.field("dayOfWeekTaken", imageEntry.getDateTaken().getDayOfWeek().getValue())
				.field("neonSiteCode", imageEntry.getSiteTaken() != null ? imageEntry.getSiteTaken().getSite().getSiteCode() : null)
				.field("position", imageEntry.getLocationTaken().getLatitude() + ", " + imageEntry.getLocationTaken().getLongitude())
				.field("altitude", imageEntry.getLocationTaken().getElevation())
				.field("droneMaker", imageEntry.getDroneMaker())
				.field("cameraModel", imageEntry.getCameraModel())
				.startObject("speed")
					.field("x", imageEntry.getSpeed().getX())
					.field("y", imageEntry.getSpeed().getY())
					.field("z", imageEntry.getSpeed().getZ())
				.endObject()
				.startObject("rotation")
					.field("roll", imageEntry.getRotation().getX())
					.field("pitch", imageEntry.getRotation().getY())
					.field("yaw", imageEntry.getRotation().getZ())
				.endObject()
			.endObject()
		.endObject();
	}

	/**
	 * Utility function used to create a JSON request body which creates a neon site entry
	 *
	 * @param boundedSite The NEON site with boundary
	 * @return A JSON creator used by ES to create a request
	 * @throws IOException IO Exception if the JSON is invalid, this shouldn't happen
	 */
	XContentBuilder makeCreateNEONSite(BoundedSite boundedSite) throws IOException
	{
		// Grab the bounded site's outer boundary which is the real polygon that makes up the boundary
		Boundary outerBoundary = boundedSite.getBoundary().getOuterBoundaryIs();
		// Grab the bounded site's inner boundary which is a list of holes inside of the outer boundary
		List<Boundary> innerBoundaries = boundedSite.getBoundary().getInnerBoundaryIs();
		// ElasticSearch assumes the first array we give it contains the outer boundary which is then followed by 0 or more inner boundary arrays
		List<Boundary> boundariesCombined = new ArrayList<>();
		// Add the outer boundary, then the inner boundaries
		boundariesCombined.add(outerBoundary);
		if (innerBoundaries != null)
			boundariesCombined.addAll(innerBoundaries);

		String siteJSON = CalliopeData.getInstance().getGson().toJson(boundedSite.getSite());

		// Start off the content builder with fields we know such as name, code, and description
		XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()
		.startObject()
			.field("site")
			.copyCurrentStructure(XContentFactory.xContent(XContentType.JSON).createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, siteJSON.getBytes()))
			.startObject("boundary")
				.field("type", "polygon")
				// Polygon coordinates are given as a 3-deep array. First an array of boundaries,
				// where each boundary is an array of locations, where each location is an array of [long, lat] positions
				.startArray("coordinates");

		// Go over each boundary
		for (Boundary boundary : boundariesCombined)
		{
			// Start the boundary array
			xContentBuilder
					.startArray();

			// Go over each coordinate in the boundary array
			for (Coordinate coordinate : boundary.getLinearRing().getCoordinates())
			{
				// Start the coordinate array, and insert [long, lat]
				xContentBuilder
						.startArray()
							.value(coordinate.getLongitude())
							.value(coordinate.getLatitude())
						.endArray();
			}

			// Finish the boundary array
			xContentBuilder
					.endArray();
		}

		// Finish the coordinates array
		xContentBuilder
				.endArray()
			.endObject()
		.endObject();
		return xContentBuilder;
	}
}
