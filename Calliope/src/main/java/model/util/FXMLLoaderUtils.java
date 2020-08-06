package model.util;

import controller.Calliope;
import javafx.fxml.FXMLLoader;

import java.io.IOException;

/**
 * Utility class for loading FXML files
 */
public class FXMLLoaderUtils
{
	/**
	 * Given a file name of a file in ./view/<FILENAME> this function loads it
	 *
	 * @param FXMLFileName The file name in /view/<FILENAME> to load
	 * @return The FXMLLoader representing that FXML file
	 */
	public static FXMLLoader loadFXML(String FXMLFileName)
	{
		FXMLLoader loader = new FXMLLoader(Calliope.class.getResource("/view/" + FXMLFileName));

		// Attempt to load the file. If we get an error throw an exception
		try
		{
			loader.load();
		}
		catch (IOException exception)
		{
			exception.printStackTrace();
			//CalliopeData.getInstance().getErrorDisplay().printError("Could not load the FXML file for the file " + FXMLFileName + "!\n" + ExceptionUtils.getStackTrace(exception));
			System.exit(-1);
		}

		// Return the result
		return loader;
	}
}
