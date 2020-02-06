package model.elevationAPI;

import model.CalliopeData;
import model.elevationAPI.jsonPOJOs.ElevationResponse;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;

public class ElevationData
{
	private static final String BASE_URL = "https://nationalmap.gov/epqs/pqs.php?x=-110&y=32&units=Meters&output=json";

	/**
	 * Grabs the height of the ground at the latitude and longitude coordinates
	 *
	 * @param latitude The latitude to test
	 * @param longitude The longitude to test
	 * @return The height of the ground at the location or -Infinity if there's a problem
	 */
	public Double getGroundElevation(Double latitude, Double longitude)
	{
		try
		{
			// Setup the correct URL
			URL elevationAPI = new URL(BASE_URL + "&x=" + longitude.toString() + "&y=" + latitude.toString());
			// Establish a connection to the elevation site
			URLConnection elevationAPIConnection = elevationAPI.openConnection();
			// Read the entire response into a buffered reader
			BufferedReader jsonReader = new BufferedReader(new InputStreamReader(elevationAPIConnection.getInputStream()));
			// Join all the lines together into a single JSON string
			String json = jsonReader.lines().collect(Collectors.joining());
			// Convert the JSON string into a structured format
			ElevationResponse elevationResponse = CalliopeData.getInstance().getGson().fromJson(json, ElevationResponse.class);
			// Make sure we got at least one response and then process it
			return elevationResponse.getResults().getElevationQuery().getElevation();
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Could not retrieve elevation at the coordinates [" + latitude + ", " + longitude + "]!\n" + ExceptionUtils.getStackTrace(e));
			return Double.NEGATIVE_INFINITY;
		}
	}
}
