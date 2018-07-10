package controller.analysisView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Font;
import model.CalliopeData;
import model.analysis.CalliopeAnalysisUtils;
import model.analysis.DataAnalyzer;
import model.location.Position;
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
		// File Name,Date Taken, Species in image, Species count, Position name, Position ID, Position latitude, Position longitude, Position elevation
		// If multiple species are in each image, the single entry is broken into multiple lines, one per species
		String rawCSV = dataAnalyzer.getImagesSortedByDate().stream().map(imageEntry ->
		{
			Position position = imageEntry.getLocationTaken();
			// Start with position name and id
			String locationString = "";

			// If we're using Lat/Lng
			if (CalliopeData.getInstance().getSettings().getLocationFormat() == SettingsData.LocationFormat.LatLong)
			{
				// Add lat/lng
				locationString = locationString +
					position.getLatitude() + "," +
					position.getLongitude() + ",";
			}
			// If we're using UTM
			else if (CalliopeData.getInstance().getSettings().getLocationFormat() == SettingsData.LocationFormat.UTM)
			{
				// Convert to UTM, and print it
				UTMCoord utmCoord = CalliopeAnalysisUtils.Deg2UTM(position.getLatitude(), position.getLongitude());
				locationString = locationString +
					utmCoord.getZone().toString() + utmCoord.getLetter().toString() + "," +
					utmCoord.getEasting() + "E," +
					utmCoord.getNorthing() + "N,";
			}
			// Add elevation
			SettingsData.DistanceUnits distanceUnits = CalliopeData.getInstance().getSettings().getDistanceUnits();
			locationString = locationString + RoundingUtils.round(distanceUnits.formatToMeters(position.getElevation()), 2) + distanceUnits.getSymbol();
			return locationString;
		}).collect(Collectors.joining("\n"));
		if (rawCSV.isEmpty())
			rawCSV = "No query results found.";
		this.txtRawCSV.setText(rawCSV);
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
}
