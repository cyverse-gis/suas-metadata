package model.settings;

import controller.Calliope;
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
	// The IP of the host
	private String elasticSearchHost;
	// The port of the host
	private Integer elasticSearchPort;
	// If the user is an admin
	private Boolean isElasticSearchAdmin;
	// If the user is an admin, the admin password
	private String elasticSearchAdminPassword;
	// If the configuration loaded successfully
	private Boolean configurationValid = false;

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
			File configFile = new File("./calliope.properties");
			if (!configFile.exists())
			{
				configFile.createNewFile();
				InputStream inputStream = Calliope.class.getResource("/calliope.properties").openStream();
				FileOutputStream outputStream = new FileOutputStream(configFile);
				IOUtils.copy(inputStream, outputStream);

				inputStream.close();
				outputStream.close();
			}

			// Open the properties file
			Configuration configuration = configs.properties(configFile);
			// Read the configuration file's host
			this.elasticSearchHost = configuration.getString("elasticSearch.host");
			// Read the configuration file's port
			this.elasticSearchPort = configuration.getInteger("elasticSearch.port", 9200);
			// If the user is an admin
			this.isElasticSearchAdmin = configuration.getBoolean("elasticSearch.admin");
			// If the user is an admin, the admin password
			this.elasticSearchAdminPassword = configuration.getString("elasticSearch.adminPassword");

			// Config is good to go
			this.configurationValid = true;
		}
		catch (ConfigurationException | IOException e)
		{
			// Print an error because the file may not exist
			System.err.println("Error parsing configuration file, elastic search database connection could not be established!\n" + ExceptionUtils.getStackTrace(e));
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
	 * @return True if we should authenticate as an ES admin
	 */
	public Boolean isElasticSearchAdmin()
	{
		return this.isElasticSearchAdmin;
	}

	/**
	 * @return The admin password
	 */
	public String getElasticSearchAdminPassword()
	{
		return this.elasticSearchAdminPassword;
	}

	/**
	 * @return True if the configuration was loaded successfully
	 */
	public Boolean isConfigurationValid()
	{
		return this.configurationValid;
	}
}
