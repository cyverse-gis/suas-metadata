package model.neon.jsonPOJOs;

/**
 * POJO class used in JSON deserialization
 */
public class Sites
{
	/*
	The field listed below is required for JSON serialization. The sites object just contains a list of sites
	 */

	private final Site[] data = null;

	public Site[] getData()
	{
		return data;
	}
}
