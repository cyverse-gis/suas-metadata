package model.site.neon.jsonPOJOs;

/**
 * POJO class used in JSON deserialization
 */
public class RawNEONSiteList
{
	/*
	The field listed below is required for JSON serialization. The sites object just contains a list of sites
	 */

	private final RawNEONSite[] data = null;

	public RawNEONSite[] getData()
	{
		return data;
	}
}
