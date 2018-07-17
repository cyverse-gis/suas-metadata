package model.elasticsearch;

import java.util.List;

/**
 * Class that represents a set of images aggregated into a bucket ready to be displayed on a map
 */
public class GeoBucket
{
	// The latitude coordinate at the center of the bucket
	private final Double centerLatitude;
	// The longitude coordinate at the center of the bucket
	private final Double centerLongitude;
	// The number of images aggregated into this bucket
	private final Long documentCount;
	// The IDs of known documents in this bucket
	private final List<String> knownDocumentIDs;

	/**
	 * Constructor just initializes fields
	 *  @param centerLatitude latitude coordinate at the center of the bucket (made up of lat averages)
	 * @param centerLongitude longitude coordinate at the center of the bucket (made up of long averages)
	 * @param documentCount The number of images aggregated into this bucket
	 * @param knownDocumentIDs The IDs of know documents in this bucket
	 */
	public GeoBucket(Double centerLatitude, Double centerLongitude, Long documentCount, List<String> knownDocumentIDs)
	{
		this.centerLatitude = centerLatitude;
		this.centerLongitude = centerLongitude;
		this.documentCount = documentCount;
		this.knownDocumentIDs = knownDocumentIDs;
	}

	///
	/// Getters only, there's no need to set any of these fields
	///

	public Double getCenterLatitude()
	{
		return centerLatitude;
	}

	public Double getCenterLongitude()
	{
		return centerLongitude;
	}

	public Long getDocumentCount()
	{
		return documentCount;
	}

	public List<String> getKnownDocumentIDs()
	{
		return knownDocumentIDs;
	}
}
