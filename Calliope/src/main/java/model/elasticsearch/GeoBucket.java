package model.elasticsearch;

public class GeoBucket
{
	private Double centerLatitude;
	private Double centerLongitude;
	private Long documentCount;

	public GeoBucket(Double centerLatitude, Double centerLongitude, Long documentCount)
	{
		this.centerLatitude = centerLatitude;
		this.centerLongitude = centerLongitude;
		this.documentCount = documentCount;
	}

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
}
