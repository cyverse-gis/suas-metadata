package model.neon;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import model.neon.jsonPOJOs.Site;

public class BoundedSite
{
	private Site site;
	private Polygon boundary;

	public BoundedSite(Site site, Polygon boundary)
	{
		this.boundary = boundary;
	}

	public Site getSite()
	{
		return this.site;
	}

	public Polygon getBoundary()
	{
		return this.boundary;
	}
}
