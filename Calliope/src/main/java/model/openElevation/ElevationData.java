package model.openElevation;

import model.CalliopeData;
import model.image.ImageEntry;
import model.settings.SensitiveConfigurationManager;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;

public class ElevationData
{
	private final String baseURL;

	/**
	 * Constructor just reads host and port from the configuration manager
	 *
	 * @param sensitiveConfigurationManager The configuration to read
	 */
	public ElevationData(SensitiveConfigurationManager sensitiveConfigurationManager)
	{
		this.baseURL = "http://" + sensitiveConfigurationManager.getElevationServerHost() + ":" + sensitiveConfigurationManager.getElevationServerPort() + "/api/v1/lookup?";
	}

	/**
	 * Compute the height of the drone above the ground using open-elevation
	 *
	 * @param imageEntry The image to compute the height above ground level of
	 * @return The height above the ground of the elevation above sea level or -1000 if there's an error
	 */
	public Double computeHeightAboveGround(ImageEntry imageEntry)
	{
		try
		{
			// Setup the correct URL
			URL openElevationAPI = new URL(baseURL + "locations=" + imageEntry.getLocationTaken().getLatitude().toString() + "," + imageEntry.getLocationTaken().getLongitude().toString());
			// Establish a connection to the open elevation site
			URLConnection openElevationConnection = openElevationAPI.openConnection();
			// Read the entire response into a buffered reader
			BufferedReader jsonReader = new BufferedReader(new InputStreamReader(openElevationConnection.getInputStream()));
			// Join all the lines together into a single JSON string
			String json = jsonReader.lines().collect(Collectors.joining());
			// Convert the JSON string into a structured format
			ElevationResponse elevationResponse = CalliopeData.getInstance().getGson().fromJson(json, ElevationResponse.class);
			// Make sure we got at least one response and then process it
			if (elevationResponse.getResults() != null && !elevationResponse.getResults().isEmpty())
			{
				Integer groundElevation = elevationResponse.getResults().get(0).getElevation();
				return imageEntry.getLocationTaken().getElevation() - groundElevation;
			}

			return -1000D;
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Could not retrieve actual height of image!\n" + ExceptionUtils.getStackTrace(e));
			return -1000D;
		}
	}

	private Double computeSensorWidth(Double focalLength, Double hFOV)
	{
		return 2 * focalLength * Math.tan(Math.toRadians(hFOV) / 2);
	}

	private Double computeSensorHeight(Double focalLength, Double vFOV)
	{
		return 2 * focalLength * Math.tan(Math.toRadians(vFOV) / 2);
	}

	public Double computeGSD(Double flightHeight, Double focalLength, Double hFOV, Double vFOV, Integer imageWidth, Integer imageHeight)
	{
		Double hGSD = (flightHeight * this.computeSensorHeight(focalLength, hFOV)) / (focalLength * imageHeight);
		Double wGSD = (flightHeight * this.computeSensorWidth(focalLength, vFOV)) / (focalLength * imageWidth);
		return Math.max(hGSD, wGSD);
	}
}
