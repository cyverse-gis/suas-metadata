package model.neon;

import de.micromata.opengis.kml.v_2_2_0.Polygon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import model.neon.jsonPOJOs.Site;

/**
 * A bounded site is simply a site with boundary
 */
public class BoundedSite
{
	// The site
	private ObjectProperty<Site> site = new SimpleObjectProperty<>();
	// The boundary of the site
	private ObjectProperty<Polygon> boundary = new SimpleObjectProperty<>();

	/**
	 * Constructor just initializes fields
	 *
	 * @param site The site this bounded site represents
	 * @param boundary The boundary of this bounded site
	 */
	public BoundedSite(Site site, Polygon boundary)
	{
		this.site.setValue(site);
		this.boundary.setValue(boundary);
	}

	/**
	 * @return Getter for site property
	 */
	public ObjectProperty<Site> siteProperty()
	{
		return this.site;
	}

	/**
	 * @return Getter for site
	 */
	public Site getSite()
	{
		return this.site.getValue();
	}

	/**
	 * @return Getter for boundary property
	 */
	public ObjectProperty<Polygon> boundaryProperty()
	{
		return this.boundary;
	}

	/**
	 * @return Getter for boundary
	 */
	public Polygon getBoundary()
	{
		return this.boundary.getValue();
	}

	/**
	 * @return We include a toString() for use in the NeonCondition filter
	 */
	@Override
	public String toString()
	{
		return this.getSite().getSiteName() + " (" + this.getSite().getSiteCode() + ")";
	}
}
