package model.elasticsearch;

import javafx.util.Pair;
import model.CalliopeData;
import model.constant.CalliopeMetadataFields;
import model.cyverse.ImageCollection;
import model.image.ImageEntry;
import model.image.VideoEntry;
import model.settings.SettingsData;
import model.site.Site;
import model.util.LocUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.elasticsearch.common.geo.builders.LineStringBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.xcontent.*;
import org.locationtech.jts.geom.Coordinate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZoneId;
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
							.startObject("siteCode")
								.field("type", "keyword")
							.endObject()
							.startObject("position")
								.field("type", "geo_point")
							.endObject()
							.startObject("elevation")
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
							.startObject("altitude")
								.field("type", "double")
							.endObject()
							.startObject("fileType")
								.field("type", "keyword")
							.endObject()
							.startObject("focalLength")
								.field("type", "double")
							.endObject()
							.startObject("width")
								.field("type", "double")
							.endObject()
							.startObject("height")
								.field("type", "double")
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
	XContentBuilder makeCalliopeSiteIndexMapping(String indexType) throws IOException
	{
		// Well, it's the builder design pattern. RIP me
		return XContentFactory.jsonBuilder()
		.startObject()
			.startObject(indexType)
				.startObject("properties")
					.startObject("name")
						.field("type", "keyword")
					.endObject()
					.startObject("code")
						.field("type", "keyword")
					.endObject()
					.startObject("boundary")
						.field("type", "geo_shape")
					.endObject()
					.startObject("type")
						.field("type", "keyword")
					.endObject()
					.startObject("details")
						.field("type", "keyword")
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
	 * Utility function used to convert a video entry to its JSON representation
	 *
	 * @param videoEntry The video to convert to its JSON representation
	 * @param collectionID The ID of the collection that the image belongs to
	 * @param fileAbsolutePath The absolute path of the file on CyVerse
	 * @return A map of key->value pairs used later in creating JSON
	 */
	XContentBuilder videoToJSON(VideoEntry videoEntry, String collectionID, String fileAbsolutePath) throws IOException
	{
		// On windows paths have \ as a path separator vs unix /. Make sure that we always use /
		String fixedAbsolutePath = fileAbsolutePath.replace('\\', '/');

		// Build the JSON representing the video metadata
		XContentBuilder builder = XContentFactory.jsonBuilder()
				.startObject()
				.field("storagePath", fixedAbsolutePath)
				.field("collectionID", collectionID)
				.startObject("imageMetadata")
				.field("dateTaken", videoEntry.getDateTaken().atZone(ZoneId.systemDefault()).format(CalliopeMetadataFields.INDEX_DATE_TIME_FORMAT))
				.field("yearTaken", videoEntry.getDateTaken().getYear())
				.field("monthTaken", videoEntry.getDateTaken().getMonthValue())
				.field("hourTaken", videoEntry.getDateTaken().getHour())
				.field("dayOfYearTaken", videoEntry.getDateTaken().getDayOfYear())
				.field("dayOfWeekTaken", videoEntry.getDateTaken().getDayOfWeek().getValue())
				.startArray("siteCode");
		for(Site site : videoEntry.getSiteTaken()) {
			builder.field(site.getCode());
		}
				builder.endArray()
				.field("position", videoEntry.getPositionTaken().getLatitude() + ", " + videoEntry.getPositionTaken().getLongitude())
				.field("elevation", videoEntry.getPositionTaken().getElevation())
				.field("droneMaker", videoEntry.getDroneMaker())
				.field("cameraModel", videoEntry.getCameraModel())
				.startObject("speed")
				.field("x", videoEntry.getSpeed().getX())
				.field("y", videoEntry.getSpeed().getY())
				.field("z", videoEntry.getSpeed().getZ())
				.endObject()
				.startObject("rotation")
				.field("roll", videoEntry.getRotation().getX())
				.field("pitch", videoEntry.getRotation().getY())
				.field("yaw", videoEntry.getRotation().getZ())
				.endObject()
				.field("altitude", videoEntry.getAltitude())
				.field("fileType", videoEntry.getFileType())
				.field("focalLength", videoEntry.getFocalLength())
				.field("width", videoEntry.getWidth())
				.field("height", videoEntry.getHeight())
				.endObject()
				.endObject();

		// We return the JSON
		return builder;
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

		// Build the JSON
		XContentBuilder builder = XContentFactory.jsonBuilder()
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
				.startArray("siteCode");
		for(Site site : imageEntry.getSiteTaken()) {
			builder.value(site.getCode());
		}
		builder.endArray()
				.field("position", imageEntry.getPositionTaken().getLatitude() + ", " + imageEntry.getPositionTaken().getLongitude())
				.field("elevation", imageEntry.getPositionTaken().getElevation())
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
				.field("altitude", imageEntry.getAltitude())
				.field("fileType", imageEntry.getFileType())
				.field("focalLength", imageEntry.getFocalLength())
				.field("width", imageEntry.getWidth())
				.field("height", imageEntry.getHeight())
			.endObject()
		.endObject();

		return builder;
	}

	/**
	 * Utility function used to create a JSON request body which creates a site entry
	 *
	 * @param site The site with boundary
	 * @return A JSON creator used by ES to create a request
	 * @throws IOException IO Exception if the JSON is invalid, this shouldn't happen
	 */
	XContentBuilder makeCreateSite(Site site) throws IOException
	{
		// Start off the content builder with fields we know such as name, code, and description
		XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()
		.startObject()
			.field("name", site.getName())
			.field("code", site.getType() + "-" + site.getCode())
			.field("type", site.getType())
			.field("boundary");

		// Utilize ElasticSearch helper classes to add the boundary polygon.
		// First we convert the list of GeoPoints into a list of coordinates.
		List<Coordinate> outerBoundary = LocUtils.geoToCoord(site.getBoundary().getOuterBoundary());
		// We then create a new polygon builder with the outer boundary and make sure to coerce the starting and ending points.
		PolygonBuilder polygonBuilder = new PolygonBuilder(new LineStringBuilder(outerBoundary), ShapeBuilder.Orientation.RIGHT, true);
		// Then, for each inner boundary, we add a hole the polygon builder represented by the inner boundary
		site.getBoundary().getInnerBoundaries().forEach(innerBoundary -> polygonBuilder.hole(new LineStringBuilder(LocUtils.geoToCoord(innerBoundary)), true));
		// Finally we write the polygon builder to our xcontent which will serialize to JSON
		polygonBuilder.toXContent(xContentBuilder, ToXContent.EMPTY_PARAMS);

		xContentBuilder
			.startArray("details");

		// Go over each detail and add key:value to the array
		for (Pair<String, ?> detail : site.getDetails())
			xContentBuilder
				.value(detail.getKey() + ":" + detail.getValue());

		// End the details array and the object
		xContentBuilder
			.endArray()
		.endObject();
		return xContentBuilder;
	}
}
