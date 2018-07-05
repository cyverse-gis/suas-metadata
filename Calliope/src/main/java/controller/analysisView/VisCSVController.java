package controller.analysisView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import model.CalliopeData;
import model.analysis.DataAnalyzer;
import model.analysis.CalliopeAnalysisUtils;
import model.location.Location;
import model.location.UTMCoord;
import model.util.RoundingUtils;
import model.util.SettingsData;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for the CSV visualization tab
 */
public class VisCSVController implements VisControllerBase
{
	///
	/// FXML bound fields start
	///

	// The text area containing raw CSV with a list of image data
	@FXML
	public TextArea txtRawCSV;
	// The text area containing location CSV with a location list
	@FXML
	public TextArea txtLocationCSV;
	// The text area containing species CSV with a species list
	@FXML
	public TextArea txtSpeciesCSV;

	///
	/// FXML bound fields end
	///

	/**
	 * Initializes the CSV controller by setting the text area fonts
	 *
	 * @param location ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		// Use a monospaced font with size 14
		this.txtRawCSV.setFont(Font.font(java.awt.Font.MONOSPACED, 14f));
		this.txtLocationCSV.setFont(Font.font(java.awt.Font.MONOSPACED, 14f));
		this.txtSpeciesCSV.setFont(Font.font(java.awt.Font.MONOSPACED, 14f));
	}

	/**
	 * Function called whenever we're given a new pre-analyzed data set to visualize
	 *
	 * @param dataAnalyzer The cloud data set to visualize
	 */
	@Override
	public void visualize(DataAnalyzer dataAnalyzer)
	{
		// The raw CSV for each image is made up of 1 line per image in the format of:
		// File Name,Date Taken, Species in image, Species count, Location name, Location ID, Location latitude, Location longitude, Location elevation
		// If multiple species are in each image, the single entry is broken into multiple lines, one per species
		String rawCSV = dataAnalyzer.getImagesSortedByDate().stream().map(imageEntry ->
		{
			Location location = imageEntry.getLocationTaken();
			// Start with location name and id
			String locationString = "";

			// If we're using Lat/Lng
			if (CalliopeData.getInstance().getSettings().getLocationFormat() == SettingsData.LocationFormat.LatLong)
			{
				// Add lat/lng
				locationString = locationString +
					location.getLatitude() + "," +
					location.getLongitude() + ",";
			}
			// If we're using UTM
			else if (CalliopeData.getInstance().getSettings().getLocationFormat() == SettingsData.LocationFormat.UTM)
			{
				// Convert to UTM, and print it
				UTMCoord utmCoord = CalliopeAnalysisUtils.Deg2UTM(location.getLatitude(), location.getLongitude());
				locationString = locationString +
					utmCoord.getZone().toString() + utmCoord.getLetter().toString() + "," +
					utmCoord.getEasting() + "E," +
					utmCoord.getNorthing() + "N,";
			}
			// Add elevation
			SettingsData.DistanceUnits distanceUnits = CalliopeData.getInstance().getSettings().getDistanceUnits();
			locationString = locationString + RoundingUtils.round(distanceUnits.formatToMeters(location.getElevation()), 2) + distanceUnits.getSymbol();
			return locationString;
		}).collect(Collectors.joining("\n"));
		if (rawCSV.isEmpty())
			rawCSV = "No query results found.";
		this.txtRawCSV.setText(rawCSV);

		// The location CSV contains each location, one per line, in the form:
		// Name, ID, Position, Elevation
		String locationCSV = dataAnalyzer.getAllImageLocations().stream().map(location ->
		{
			// Location name and ID
			String locationString = "";

			// If we're using lat long
			if (CalliopeData.getInstance().getSettings().getLocationFormat() == SettingsData.LocationFormat.LatLong)
			{
				// Use lat,lng
				locationString = locationString +
					location.getLatitude() + "," +
					location.getLongitude() + ",";
			}
			// If we're using UTM
			else
			{
				UTMCoord utmCoord = CalliopeAnalysisUtils.Deg2UTM(location.getLatitude(), location.getLongitude());
				locationString = locationString +
					utmCoord.getZone().toString() + utmCoord.getLetter().toString() + "," +
					utmCoord.getEasting() + "E," +
					utmCoord.getNorthing() + "N,";
			}
			// Distance units depend on feet or meters
			SettingsData.DistanceUnits distanceUnits = CalliopeData.getInstance().getSettings().getDistanceUnits();
			locationString = locationString + RoundingUtils.round(distanceUnits.formatToMeters(location.getElevation()), 2) + distanceUnits.getSymbol();
			return locationString;
		})
		.collect(Collectors.joining("\n"));
		this.txtLocationCSV.setText(locationCSV);

		// The species CSV contains each species, one per line, in the form:
		// Name, Scientific Name, Key bound (or null if none)
		String speciesCSV = dataAnalyzer.getAllImageSpecies().stream().map(species ->
			species.getCommonName() + "," +
			species.getScientificName()
		).collect(Collectors.joining("\n"));
		this.txtSpeciesCSV.setText(speciesCSV);
	}

	/**
	 * If copy CSV is pressed, we copy the content of the CSV clipboard
	 *
	 * @param actionEvent consumed
	 */
	public void copyRawCSV(ActionEvent actionEvent)
	{
		ClipboardContent content = new ClipboardContent();
		content.putString(this.txtRawCSV.getText());
		Clipboard.getSystemClipboard().setContent(content);
		actionEvent.consume();
	}

	/**
	 * If copy Locations CSV is pressed, we copy the content of the CSV clipboard
	 *
	 * @param actionEvent consumed
	 */
	public void copyLocationsCSV(ActionEvent actionEvent)
	{
		ClipboardContent content = new ClipboardContent();
		content.putString(this.txtLocationCSV.getText());
		Clipboard.getSystemClipboard().setContent(content);
		actionEvent.consume();
	}

	/**
	 * If copy Species CSV is pressed, we copy the content of the CSV clipboard
	 *
	 * @param actionEvent consumed
	 */
	public void copySpeciesCSV(ActionEvent actionEvent)
	{
		ClipboardContent content = new ClipboardContent();
		content.putString(this.txtSpeciesCSV.getText());
		Clipboard.getSystemClipboard().setContent(content);
		actionEvent.consume();
	}
}
