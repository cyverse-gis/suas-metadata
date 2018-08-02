package model.site.neon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.util.Pair;
import model.image.ImageEntry;
import model.site.Boundary;
import model.site.Site;
import model.site.neon.jsonPOJOs.RawNEONProductAvailability;

/**
 * A NEON site is just a site that was provided by NEON
 */
public class NEONSite extends Site
{
	// Extra fields provided by the NEON site
	private final StringProperty domainName = new SimpleStringProperty(null);
	private final StringProperty domainCode = new SimpleStringProperty(null);
	private final StringProperty siteDescription = new SimpleStringProperty(null);
	private final StringProperty siteType = new SimpleStringProperty(null);
	private final StringProperty stateCode = new SimpleStringProperty(null);
	private final StringProperty stateName = new SimpleStringProperty(null);
	private transient ObjectProperty<RawNEONProductAvailability[]> dataProducts = new SimpleObjectProperty<>();

	// A constant neon pin icon that we cache so we dont have to load the image over and over again
	private static final Image NEON_ICON = new Image(ImageEntry.class.getResource("/images/mapWindow/neonIcon32.png").toString());
	// A constant highlighted neon pin icon that we cache so we dont have to load the image over and over again
	private static final Image NEON_HIGHLIGHTED_ICON = new Image(ImageEntry.class.getResource("/images/mapWindow/highlightedNeonIcon32.png").toString());

	/**
	 * Constructor takes the site's name, code, and boundary as input
	 *
	 * @param siteName The site's name
	 * @param siteCode The site's code
	 * @param boundary The site's boundary
	 */
	public NEONSite(String siteName, String siteCode, Boundary boundary)
	{
		super(siteName, siteCode, "NEON", boundary);
	}

	/**
	 * Getter for icon of this site, in our case it's the NEON icon
	 *
	 * @param hovered If the site is hovered or not
	 * @return An image based on if the site was hovered or not
	 */
	@Override
	public Image getIcon(Boolean hovered)
	{
		if (hovered)
			return NEON_HIGHLIGHTED_ICON;
		else
			return NEON_ICON;
	}

	/**
	 * Getter for list of details that this site provides
	 *
	 * @return A list of details that this site provides as key, value pairs
	 */
	@Override
	public ObservableList<Pair<String, ?>> getDetails()
	{
		return FXCollections.observableArrayList(
			new Pair<>("domainName", this.getDomainName()),
			new Pair<>("domainCode", this.getDomainCode()),
			new Pair<>("siteDescription", this.getSiteDescription()),
			new Pair<>("siteType", this.getSiteType()),
			new Pair<>("stateCode", this.getStateCode()),
			new Pair<>("stateName", this.getStateName())
		);
	}

	///
	/// Getters and Setters for NEON specific properties
	///

	public void setDomainCode(String domainCode)
	{
		this.domainCode.setValue(domainCode);
	}
	public String getDomainCode()
	{
		return domainCode.get();
	}
	public StringProperty domainCodeProperty()
	{
		return domainCode;
	}

	public void setDomainName(String domainName)
	{
		this.domainName.setValue(domainName);
	}
	public String getDomainName()
	{
		return domainName.get();
	}
	public StringProperty domainNameProperty()
	{
		return domainName;
	}

	public void setSiteDescription(String siteDescription)
	{
		this.siteDescription.setValue(siteDescription);
	}
	public String getSiteDescription()
	{
		return siteDescription.get();
	}
	public StringProperty siteDescriptionProperty()
	{
		return siteDescription;
	}

	public void setSiteType(String siteType)
	{
		this.siteType.setValue(siteType);
	}
	public String getSiteType()
	{
		return siteType.get();
	}
	public StringProperty siteTypeProperty()
	{
		return siteType;
	}

	public void setStateCode(String stateCode)
	{
		this.stateCode.setValue(stateCode);
	}
	public String getStateCode()
	{
		return stateCode.get();
	}
	public StringProperty stateCodeProperty()
	{
		return stateCode;
	}

	public void setStateName(String stateName)
	{
		this.stateName.setValue(stateName);
	}
	public String getStateName()
	{
		return stateName.get();
	}
	public StringProperty stateNameProperty()
	{
		return stateName;
	}

	public void setDataProducts(RawNEONProductAvailability[] dataProducts)
	{
		this.dataProducts.setValue(dataProducts);
	}
	public RawNEONProductAvailability[] getDataProducts()
	{
		return dataProducts.get();
	}
	public ObjectProperty<RawNEONProductAvailability[]> dataProductsProperty()
	{
		return dataProducts;
	}
}
