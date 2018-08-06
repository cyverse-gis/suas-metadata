package model.util;

import org.elasticsearch.common.geo.GeoPoint;
import org.locationtech.jts.geom.Coordinate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Set of utility functions to convert between the different coordinate classes in all libraries
 */
public class LocUtils
{
	/**
	 * Converts from an ElasticSearch GeoPoint to a LocationTech Coordinate
	 *
	 * @param geoPoint The GeoPoint to convert
	 * @return A coordinate representing this point
	 */
	public static Coordinate geoToCoord(GeoPoint geoPoint)
	{
		return new Coordinate(geoPoint.getLon(), geoPoint.getLat());
	}

	/**
	 * Converts from a list of ElasticSearch GeoPoints to a list of LocationTech Coordinates
	 *
	 * @param geoPoints The GeoPoints to convert
	 * @return A list of coordinates representing these points
	 */
	public static List<Coordinate> geoToCoord(List<GeoPoint> geoPoints)
	{
		return geoPoints.stream().map(LocUtils::geoToCoord).collect(Collectors.toList());
	}
}
