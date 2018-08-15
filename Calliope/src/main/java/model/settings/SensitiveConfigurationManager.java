package model.settings;

import controller.Calliope;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import model.util.ErrorDisplay;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
/**
 * Class used to wrap a configuration file containing a port and host IP
 */
public class SensitiveConfigurationManager
{
	// The IP of the ES host
	private String elasticSearchHost;
	// The port of the ES host
	private Integer elasticSearchPort;
	// If the configuration loaded successfully
	private BooleanProperty configurationValid = new SimpleBooleanProperty(false);

	/**
	 * Constructor reads the configuration file and initializes fields
	 *
	 * @param errorDisplay The error displayer to print errors to
	 */
	public SensitiveConfigurationManager(ErrorDisplay errorDisplay)
	{
		// Create a config factory helper object
		Configurations configs = new Configurations();
		try
		{
			// Grab the config file from the properties file
			File configFile = new File("./calliope.properties");
			// If the config file does not exist yet, create a default
			if (!configFile.exists())
			{
				// Create the file
				configFile.createNewFile();
				// Open a stream to our default
				InputStream inputStream = Calliope.class.getResource("/calliope.properties").openStream();
				FileOutputStream outputStream = new FileOutputStream(configFile);
				IOUtils.copy(inputStream, outputStream);

				inputStream.close();
				outputStream.close();
			}

			// Open the properties file
			Configuration configuration = configs.properties(configFile);
			// Read the configuration file's ES host
			this.elasticSearchHost = configuration.getString("elasticSearch.host");
			// Read the configuration file's ES port
			this.elasticSearchPort = configuration.getInteger("elasticSearch.port", 9200);

			// Config is good to go
			if (this.elasticSearchHost != null)
				this.configurationValid.setValue(true);
			// Config is not good, so show an error
			else
				errorDisplay.notify("Invalid ElasticSearch host or port, please check `calliope.properties`!");
		}
		catch (ConfigurationException | IOException e)
		{
			// Print an error because the file may not exist
			errorDisplay.notify("Error parsing configuration file, elastic search database connection could not be established!\n" + ExceptionUtils.getStackTrace(e));
		}
		catch (ConversionException e)
		{
			errorDisplay.notify("Invalid ElasticSearch host or port, please check `calliope.properties`!");
		}
	}

	/**
	 * @return Getter for ES host IP
	 */
	public String getElasticSearchHost()
	{
		return this.elasticSearchHost;
	}

	/**
	 * @return Getter for ES host port
	 */
	public Integer getElasticSearchPort()
	{
		return this.elasticSearchPort;
	}

	/**
	 * @return True if the configuration was loaded successfully
	 */
	public Boolean isConfigurationValid()
	{
		return this.configurationValid.getValue();
	}

	/**
	 * @return The configuration valid property
	 */
	public BooleanProperty configurationValidProperty()
	{
		return configurationValid;
	}
}
