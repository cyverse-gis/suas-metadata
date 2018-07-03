package model.neon;

import de.micromata.opengis.kml.v_2_2_0.Polygon;
import model.neon.jsonPOJOs.Site;

/**
 * A bounded site is simply a site with boundary
 */
public class BoundedSite
{
	// The site
	private Site site;
	// The boundary of the site
	private Polygon boundary;

	/**
	 * Constructor just initializes fields
	 *
	 * @param site The site this bounded site represents
	 * @param boundary The boundary of this bounded site
	 */
	public BoundedSite(Site site, Polygon boundary)
	{
		this.site = site;
		this.boundary = boundary;
	}

	/**
	 * @return Getter for site
	 */
	public Site getSite()
	{
		return this.site;
	}

	/**
	 * @return Getter for boundary
	 */
	public Polygon getBoundary()
	{
		return this.boundary;
	}
}
