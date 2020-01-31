package model.site.usfs;

import com.vividsolutions.jts.geom.*;
import model.CalliopeData;
import model.site.Boundary;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.Name;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class used to manage and extract LTAR data from a shape file
 */
public class USFSData
{
	/**
	 * Parses a shapefile into a list of sites
	 *
	 * @return A list of sites + their boundaries
	 */
	public List<USFSSite> retrieveSites()
	{
		// A list of sites to return
		List<USFSSite> toReturn = new ArrayList<>();

		try
		{


			// Grab the polygon shape file
			String shapeFilePath = USFSData.class.getResource( "/files/usfs_data/USFSAdminBoundaries.shp").toString();

			// Create a connection map that is used by the datastore finder to open the shape file
			Map<String, String> connect = new HashMap<>();
			// The URL value is just a reference to our shape file
			connect.put("url", shapeFilePath);



			// 'Connect' to the file by opening the local file
			DataStore dataStore = DataStoreFinder.getDataStore(connect);
			// Grab all the type names, in our USFS case we should only get 1 type name
			String[] typeNames = dataStore.getTypeNames();


			// Iterate over the type names (should only be 1)
			for (String typeName : typeNames)
			{
				// Grab required fields from the data store and open an iterator to those fields
				FeatureSource featureSource = dataStore.getFeatureSource(typeName);
				FeatureCollection featureCollection = featureSource.getFeatures();
				FeatureIterator featureIterator = featureCollection.features();

				// While our iterator has another entry, read it
				while (featureIterator.hasNext())
				{
					// Create a new site that will temporary store all the fields we read from the file
					USFSSite site = new USFSSite();
					// Each site can have infinite boundaries, so keep a list of those boundaries here. Later we create 1 site per boundary
					List<Boundary> boundaries = new ArrayList<>();
					// Grab the next feature from the iterator
					Feature next = featureIterator.next();
					// The feature should have a collection of properties
					Collection<Property> properties = next.getProperties();
					// Go over each property
					for (Property property : properties)
					{
						// Each property has a name (key) and a value
						Name name = property.getName();
						Object value = property.getValue();

						// Test the name or key of the property and decode the value accordingly
						switch(name.toString())
						{
							// If the key is the geometry, parse the geometry which should be a multi-polygon
							case "the_geom":
								if (value instanceof MultiPolygon)
								{
									// Grab our multi-value polygon
									MultiPolygon multiPolygon = (MultiPolygon) value;
									// Iterate over each geometry in the polygon
									for (int index = 0; index < multiPolygon.getNumGeometries(); index++)
									{
										// Grab the geometry at the corresponding index
										Geometry geometry = multiPolygon.getGeometryN(index);
										// The geometry should be a polygon, if not print an error
										if (geometry instanceof Polygon)
										{
											// Cast the polygon
											Polygon polygon = (Polygon) geometry;
											// Grab the exterior ring. This should be a ring of coordinates
											LineString exteriorRing = polygon.getExteriorRing();
											// Grab the interior rings. This should be a list of holes
											List<LineString> interiorRings = new ArrayList<>();
											// For each interior ring add it to our list
											for (int i = 0; i < polygon.getNumInteriorRing(); i++)
												interiorRings.add(polygon.getInteriorRingN(i));
											// Add a new boundary for this site which has exactly one outer boundary and 0 to inf inner boundaries
											boundaries.add(new Boundary(
													rawToStructured(exteriorRing.getCoordinates()),
													interiorRings.stream().map(interiorRing -> rawToStructured(interiorRing.getCoordinates())).collect(Collectors.toList())));
										}
										else
										{
											CalliopeData.getInstance().getErrorDisplay().printError("Got a multipolygon that contained a non-polygon entry, this should not happen. It was a: " + geometry.getClass().getSimpleName());
										}
									}
								}
								break;
							// If the key is a forest name test if it's a string and if so set the site acronym
							case "FORESTNAME":
								if (value instanceof String)
									site.setForestName((String) value);
								break;
							// If the key is the site's full name test if it's a string and if so set the site name
							case "sitenamefu":
								if (value instanceof String)
									site.setName((String) value);
								break;
							// If the key is a region test if it's a string and if so set the site city
							case "REGION":
								if (value instanceof String)
									site.setRegion((String) value);
								break;
							// If the key is the site's code test if it's an integer and if so set the site code
							case "aite_id":
								if (value instanceof Integer)
									site.setCode(value.toString());
								break;
							// Some keys are thrown away, do so here
							default:
								break;
						}
					}
					// Once we've parsed all possible information from the entry, iterate over all the boundaries this site has. Create
					// one site per boundary and add it to the list.
					boundaries.forEach(boundary ->
					{
						USFSSite subSite = new USFSSite();
						subSite.setForestName(site.getForestName());
						subSite.setRegion(site.getRegion());
						subSite.setBoundary(boundary);
						subSite.setCode(site.getCode());
						subSite.setName(site.getName());
						subSite.setType(site.getType());
						subSite.setCenter(site.getCenter());
						toReturn.add(subSite);
					});
				}
				// Close the iterator to close the file
				featureIterator.close();
			}
		}
		// The shape file could not be read so print out an error
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Could not parse the shape file. Error was:\n" + ExceptionUtils.getStackTrace(e));
		}

		return toReturn;
	}

	/**
	 * Converts an array of coordinates to a list of GeoPoints. Both coordinates and geopoints store lat/longs but are from different libraries
	 *
	 * @param coordinates The array of coordinates to convert
	 * @return A list of geo-points representing the array of coordinates
	 */
	private List<GeoPoint> rawToStructured(Coordinate[] coordinates)
	{
		// Just map each coordinate to a new geopoint, remembering that y and x are flipped.
		return Arrays.stream(coordinates).map(coordinate -> new GeoPoint(coordinate.y, coordinate.x)).collect(Collectors.toList());
	}
}