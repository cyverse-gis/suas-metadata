package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import model.CalliopeData;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.PropertySheet;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicObservableValue;

/**
 * Controller for the settings tab
 */
public class CalliopeSettingsController
{
	///
	/// FXML bound fields start
	///

	// The sheet of Calliope properties
	@FXML
	public PropertySheet pstSettings;

	// Bottom label telling us if ExifTool loaded successfully
	@FXML
	public Label lblExifToolStatus;
	// Bottom text box giving us install instructions if ExifTool was not found on a user's system
	@FXML
	public TextArea txtInstallInstructions;

	///
	/// FXML bound fields end
	///

	/**
	 * Initialize sets up the analysis window and bindings
	 */
	@FXML
	public void initialize()
	{
		// Bind the Calliope settings list to the property sheet settings because we cant bind the list property...
		EasyBind.listBind(this.pstSettings.getItems(), CalliopeData.getInstance().getSettings().getSettingList());

		// Create a monadic binding out of the exif tool's found property, and then map it accordingly
		MonadicObservableValue<Boolean> exifToolFoundProperty = EasyBind.monadic(CalliopeData.getInstance().getMetadataManager().exifToolFoundProperty());
		// If we have exiftool, display "Found" otherwise display "Not Found"
		this.lblExifToolStatus.textProperty().bind(exifToolFoundProperty.map(exifToolFound -> exifToolFound ? "Found" : "Not Found"));
		// Show "Found" in green and "Not Found" in red
		this.lblExifToolStatus.textFillProperty().bind(exifToolFoundProperty.map(exifToolFound -> exifToolFound ? Color.GREEN : Color.RED));
		// If exiftool isn't found, show install instructions
		this.txtInstallInstructions.visibleProperty().bind(exifToolFoundProperty.map(exifToolFound -> !exifToolFound));

		// A string containing install instructions
		String installInstructions;
		// Show mac install instructions
		if (SystemUtils.IS_OS_MAC)
		{
			installInstructions =
				"You need ExifTool installed on your system to import images and read their metadata. Please install ExifTool and make sure it is in your $PATH.\n" +
				"MacOS Instructions:\n" +
				"1. Download ExifTool 11.06 from 'https://www.sno.phy.queensu.ca/~phil/exiftool/ExifTool-11.06.dmg' or get a newer version from 'https://www.sno.phy.queensu.ca/~phil/exiftool/'\n" +
				"2. Install ExifTool by double clicking the dmg file and follow the instructions on screen\n" +
				"3. To ensure you have installed ExifTool successfully, open a terminal and type 'exiftool'. If you get 'Command not found' there was an issue with the installation. If you get 'Syntax:  exiftool [OPTIONS] FILE' you have installed ExifTool successfully\n" +
				"4. Restart Calliope, and the ExifTool status should read 'Found'";
		}
		// Show Linux install instructions
		else if (SystemUtils.IS_OS_LINUX)
		{
			installInstructions =
				"You need ExifTool installed on your system to import images and read their metadata. Please install ExifTool and make sure it is in your $PATH.\n" +
				"Linux Instructions:\n" +
				"\n" +
				"Ubuntu:\n" +
				"1. Run the command 'sudo apt-get install libimage-exiftool-perl'\n" +
				"2. To ensure you have installed ExifTool successfully, open a terminal and type 'exiftool'. If you get 'Command not found' there was an issue with the installation. If you get 'Syntax:  exiftool [OPTIONS] FILE' you have installed ExifTool successfully\n" +
				"3. Restart Calliope, and the ExifTool status should read 'Found'\n" +
				"\n" +
				"Other Linux Flavors:\n" +
				"1. Download ExifTool 11.06 from 'https://www.sno.phy.queensu.ca/~phil/exiftool/Image-ExifTool-11.06.tar.gz' or get a newer version from 'https://www.sno.phy.queensu.ca/~phil/exiftool/'\n" +
				"2. Navigate to the download directory with 'cd <download directory>'\n" +
				"3. Extract the tar.gz file with 'gzip -dc Image-ExifTool-11.06.tar.gz | tar -xf -'\n" +
				"4. Move into the extracted directory with 'cd Image-ExifTool-11.06' (You may have a different version number)\n" +
				"5. Compile ExifTool with 'perl Makefile.PL'\n" +
				"6. Install Exiftool with 'sudo make install'\n" +
				"7. To ensure you have installed ExifTool successfully, open a terminal and type 'exiftool'. If you get 'Command not found' there was an issue with the installation. If you get 'Syntax:  exiftool [OPTIONS] FILE' you have installed ExifTool successfully\n" +
				"8. Restart Calliope, and the ExifTool status should read 'Found'";
		}
		else
		{
			installInstructions = "You need ExifTool installed on your system to import images and read their metadata. Please install ExifTool and make sure it is in your $PATH.\nJava was unable to detect your operating system, please see 'https://www.sno.phy.queensu.ca/~phil/exiftool/' for instructions on how to install ExifTool";
		}

		// Set text based on OS
		this.txtInstallInstructions.setText(installInstructions);
	}
}
