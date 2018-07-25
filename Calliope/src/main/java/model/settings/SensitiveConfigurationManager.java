package model.settings;

import controller.Calliope;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
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
	// The user's ES username
	private String elasticSearchUsername;
	// The user's ES password
	private String elasticSearchPassword;

	/**
	 * Constructor reads the configuration file and initializes fields
	 */
	public SensitiveConfigurationManager()
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
			// Read the configuration file's username
			this.elasticSearchUsername = configuration.getString("elasticSearch.username");
			// Read the configuration file's password
			this.elasticSearchPassword = configuration.getString("elasticSearch.password");
		}
		catch (ConfigurationException | IOException e)
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

	/**
	 * @return Getter for ES username
	 */
	public String getElasticSearchUsername()
	{
		return this.elasticSearchUsername;
	}

	/**
	 * @return Getter for ES password
	 */
	public String getElasticSearchPassword()
	{
		return this.elasticSearchPassword;
	}
}
