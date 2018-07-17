package model.settings;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;

/**
 * Class used to wrap a configuration file containing a port and host IP
 */
public class SensitiveConfigurationManager
{
	// The IP of the host
	private String elasticSearchHost;
	// The port of the host
	private Integer elasticSearchPort;

	/**
	 * Constructor reads the configuration file and initializes fields
	 */
	public SensitiveConfigurationManager()
	{
		// Create a config factory helper object
		Configurations configs = new Configurations();
		try
		{
			// Open the properties file
			Configuration configuration = configs.properties(new File("calliope.properties"));
			// Read the configuration file's host
			this.elasticSearchHost = configuration.getString("elasticSearch.host");
			// Read the configuration file's port
			this.elasticSearchPort = configuration.getInteger("elasticSearch.port", 9200);
		}
		catch (ConfigurationException e)
		{
			// Print an error because the file may not exist
			System.err.println("Error parsing configuration file, elastic search database connection could not be established!\n" + ExceptionUtils.getStackTrace(e));
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
}
