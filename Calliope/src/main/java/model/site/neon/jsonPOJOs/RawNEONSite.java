package model.site.neon.jsonPOJOs;

import javafx.beans.property.*;

/**
 * POJO class used in JSON deserialization
 */
public class RawNEONSite
{
	/*
	All fields below are listed because they are required for JSON serialization. Only a few of the fields are actually used.
	Getters are found below too, but mostly unused.
	 */

	private final StringProperty domainCode = new SimpleStringProperty(null);
	private final StringProperty domainName = new SimpleStringProperty(null);
	private final StringProperty siteCode = new SimpleStringProperty(null);
	private final StringProperty siteDescription = new SimpleStringProperty(null);
	private final DoubleProperty siteLatitude = new SimpleDoubleProperty(0);
	private final DoubleProperty siteLongitude = new SimpleDoubleProperty(0);
	private final StringProperty siteName = new SimpleStringProperty(null);
	private final StringProperty siteType = new SimpleStringProperty(null);
	private final StringProperty stateCode = new SimpleStringProperty(null);
	private final StringProperty stateName = new SimpleStringProperty(null);
	private transient ObjectProperty<RawNEONProductAvailability[]> dataProducts = new SimpleObjectProperty<>();

	/**
	 * Utility function to be called if this class is deserialized from JSON
	 */
	public void initFromJSON()
	{
		this.dataProducts = new SimpleObjectProperty<>();
	}

	public String getDomainCode()
	{
		return domainCode.get();
	}

	public StringProperty domainCodeProperty()
	{
		return domainCode;
	}

	public String getDomainName()
	{
		return domainName.get();
	}

	public StringProperty domainNameProperty()
	{
		return domainName;
	}

	public String getSiteCode()
	{
		return siteCode.get();
	}

	public StringProperty siteCodeProperty()
	{
		return siteCode;
	}

	public String getSiteDescription()
	{
		return siteDescription.get();
	}

	public StringProperty siteDescriptionProperty()
	{
		return siteDescription;
	}

	public double getSiteLatitude()
	{
		return siteLatitude.get();
	}

	public DoubleProperty siteLatitudeProperty()
	{
		return siteLatitude;
	}

	public double getSiteLongitude()
	{
		return siteLongitude.get();
	}

	public DoubleProperty siteLongitudeProperty()
	{
		return siteLongitude;
	}

	public String getSiteName()
	{
		return siteName.get();
	}

	public StringProperty siteNameProperty()
	{
		return siteName;
	}

	public String getSiteType()
	{
		return siteType.get();
	}

	public StringProperty siteTypeProperty()
	{
		return siteType;
	}

	public String getStateCode()
	{
		return stateCode.get();
	}

	public StringProperty stateCodeProperty()
	{
		return stateCode;
	}

	public String getStateName()
	{
		return stateName.get();
	}

	public StringProperty stateNameProperty()
	{
		return stateName;
	}

	public RawNEONProductAvailability[] getDataProducts()
	{
		return dataProducts.get();
	}

	public ObjectProperty<RawNEONProductAvailability[]> dataProductsProperty()
	{
		return dataProducts;
	}

	/**
	 * To string returns the site as a more readable string
	 *
	 * @return Print the object in the format '<name> at <latitude>, <longitude>'
	 */
	@Override
	public String toString()
	{
		return this.siteName.getValue() + " at " + this.siteLatitude.getValue().toString() + ", " + this.siteLongitude.getValue().toString();
	}
}
