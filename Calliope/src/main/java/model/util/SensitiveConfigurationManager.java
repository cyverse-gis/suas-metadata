package model.util;

import model.CalliopeData;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;

public class SensitiveConfigurationManager
{
	private String elasticSearchHost;
	private Integer elasticSearchPort;

	public SensitiveConfigurationManager()
	{
		Configurations configs = new Configurations();
		try
		{
			Configuration configuration = configs.properties(new File("calliope.properties"));
			this.elasticSearchHost = configuration.getString("elasticSearch.host");
			this.elasticSearchPort = configuration.getInteger("elasticSearch.port", 9200);
		}
		catch (ConfigurationException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Error parsing configuration file, elastic search database connection could not be established!\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	public String getElasticSearchHost()
	{
		return this.elasticSearchHost;
	}

	public Integer getElasticSearchPort()
	{
		return this.elasticSearchPort;
	}
}
