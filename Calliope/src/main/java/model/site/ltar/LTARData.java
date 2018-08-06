package model.site.ltar;

import com.vividsolutions.jts.geom.*;
import model.site.Boundary;
import org.elasticsearch.common.geo.GeoPoint;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LTARData
{
	/**
	 * Parses a shapefile into a list of sites
	 *
	 * @return A list of sites + their boundaries
	 */
	public List<LTARSite> retrieveSites()
	{
		List<LTARSite> toReturn = new ArrayList<>();

		try
		{
			File shapeFile = new File(LTARData.class.getResource("/files/ltar_data/ltar_site_polygon.shp").getFile());

			Map<String, String> connect = new HashMap<>();
			connect.put("url", shapeFile.toURI().toString());

			DataStore dataStore = DataStoreFinder.getDataStore(connect);
			String[] typeNames = dataStore.getTypeNames();

			for (String typeName : typeNames)
			{
				FeatureSource featureSource = dataStore.getFeatureSource(typeName);
				FeatureCollection featureCollection = featureSource.getFeatures();
				FeatureIterator featureIterator = featureCollection.features();

				while (featureIterator.hasNext())
				{
					LTARSite site = new LTARSite();
					Feature next = featureIterator.next();
					Collection<Property> properties = next.getProperties();
					for (Property property : properties)
					{
						Name name = property.getName();
						Object value = property.getValue();

						switch(name.toString())
						{
							case "the_geom":
								if (value instanceof MultiPolygon)
								{
									MultiPolygon multiPolygon = (MultiPolygon) value;
									Geometry boundary = multiPolygon.getGeometryN(0);
									if (boundary instanceof Polygon)
									{
										Polygon polygon = (Polygon) boundary;
										LineString exteriorRing = polygon.getExteriorRing();
										List<LineString> interiorRings = new ArrayList<>();
										for (int i = 0; i < polygon.getNumInteriorRing(); i++)
											interiorRings.add(polygon.getInteriorRingN(i));
										site.setBoundary(new Boundary(
											rawToStructured(exteriorRing.getCoordinates()),
											interiorRings.stream().map(interiorRing -> rawToStructured(interiorRing.getCoordinates())).collect(Collectors.toList())));
									}
									else
									{
										System.out.println("Not a polygon? " + boundary.getClass().getSimpleName());
									}
								}
								break;
							case "acronym":
								if (value instanceof String)
									site.setAcronym((String) value);
								break;
							case "sitenamefu":
								if (value instanceof String)
									site.setName((String) value);
								break;
							case "location":
								if (value instanceof String)
									site.setCity((String) value);
								break;
							case "locationst":
								if (value instanceof String)
									site.setState((String) value);
								break;
							case "site_id":
								if (value instanceof Integer)
									site.setCode(value.toString());
								break;
							default:
								break;
						}
					}
					toReturn.add(site);
				}
				featureIterator.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return toReturn;
	}

	private List<GeoPoint> rawToStructured(Coordinate[] coordinates)
	{
		return Arrays.stream(coordinates).map(coordinate -> new GeoPoint(coordinate.y, coordinate.x)).collect(Collectors.toList());
	}
}