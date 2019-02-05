package model.site.neon;

import de.micromata.opengis.kml.v_2_2_0.*;
import de.micromata.opengis.kml.v_2_2_0.gx.Tour;
import model.CalliopeData;
import model.site.Boundary;
import model.site.neon.jsonPOJOs.RawNEONSite;
import model.site.neon.jsonPOJOs.RawNEONSiteList;
import model.util.AnalysisUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.geo.GeoPoint;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Neon data object contains utility functions for interacting with the NEON api
 */
public class NeonData
{
	// Base URL for the NEON api
	private static final String NEON_API_URL = "http://data.neonscience.org/data-api/#/";
	// A hard coded link to the NEON KMZ file containing all locations + boundaries
	private static final String NEON_KMZ_LINK = "https://www.neonscience.org/sites/default/files/NEON-Project-Locations-v16_1.kmz";

	/**
	 * Parses a KML document into a list of sites
	 *
	 * @return A list of sites + their boundaries
	 */
	public List<NEONSite> retrieveSites()
	{
		// Create a list of sites to add to
		List<NEONSite> neonSites = new ArrayList<>();

		// Grab the current list of sites from the NEON API
		List<RawNEONSite> rawNEONSites = this.pullSites();

		// Grab the current KML from the NEON website
		Kml kml = this.getCurrentSiteKML();

		if (kml != null)
		{
			// The top level directory contains a single directory which should have NEON project locations. Check that here
			Feature lvl0Feature = kml.getFeature();
			if (lvl0Feature.getName().equals("NEON Project Locations (v16)") && lvl0Feature instanceof Folder)
			{
				// Cast the feature into a folder since we tested above
				Folder topFolder = (Folder) lvl0Feature;
				// In the top folder there are 6 sub-folders, and we are interested in a single one. Search for it here
				for (Feature lvl1Feature : topFolder.getFeature())
				{
					// If the feature has the right name and type, we found it!
					if (lvl1Feature.getName().equals("NEON_Field_Sampling_Boundaries") && lvl1Feature instanceof Document)
					{
						// Parse the feature into a document which we tested above
						Document fieldSamplingBoundaries = (Document) lvl1Feature;
						// This document should have two features, one list of names & positions and the other a list of boundaries
						// Grab both features
						Feature lvl2FeaturePoly = fieldSamplingBoundaries.getFeature().stream().filter(lvl2Feature -> lvl2Feature.getName().equals("Features")).findFirst().get();
						Feature lvl2FeatureLabl = fieldSamplingBoundaries.getFeature().stream().filter(lvl2Feature -> lvl2Feature.getName().equals("Feature Labels (Name)")).findFirst().get();
						// Test if they are non-null and the right type
						if (lvl2FeaturePoly instanceof Folder && lvl2FeatureLabl instanceof Folder)
						{
							// The first feature should have a list of polygons, and the second feature should have a list of labels
							List<Feature> polygons = ((Folder) lvl2FeaturePoly).getFeature();
							List<Feature> labels = ((Folder) lvl2FeatureLabl).getFeature();
							// These lists should be the same size as they contain parallel data, so ensure that here
							if (polygons.size() == labels.size() && polygons.stream().allMatch(feature -> feature instanceof Placemark) && labels.stream().allMatch(label -> label instanceof Placemark))
							{
								// Iterate over parallel data
								for (Integer i = 0; i < polygons.size(); i++)
								{
									// Grab one entry from both lists
									Placemark polygonPlacemark = (Placemark) polygons.get(i);
									Placemark labelPlacemark = (Placemark) labels.get(i);
									// Grab the geometry from both lists, one should be a polygon and the other should be a point
									Geometry polygonRaw = polygonPlacemark.getGeometry();
									Geometry labelRaw = labelPlacemark.getGeometry();
									// Ensure the geometry has the right type
									if (polygonRaw instanceof Polygon && labelRaw instanceof Point)
									{
										// Grab the point and polygon
										Polygon boundary = (Polygon) polygonRaw;
										Point location = (Point) labelRaw;
										// A point contains a list of coordinates, which contains exactly one element, grab it
										Coordinate locationCoord = location.getCoordinates().get(0);
										// Find the closest site to this location coordinate
										RawNEONSite rawNEONSite = this.closestSiteTo(rawNEONSites, locationCoord.getLatitude(), locationCoord.getLongitude());
										// Make sure the site is non-null
										if (rawNEONSite != null)
										{
											// Compute the outer and inner  boundaries
											List<GeoPoint> outerBoundary = boundary.getOuterBoundaryIs().getLinearRing().getCoordinates().stream().map(coordinate -> new GeoPoint(coordinate.getLatitude(), coordinate.getLongitude())).collect(Collectors.toList());
											List<List<GeoPoint>> innerBoundaries = boundary.getInnerBoundaryIs().stream().map(innerBoundary -> innerBoundary.getLinearRing().getCoordinates().stream().map(coordinate -> new GeoPoint(coordinate.getLatitude(), coordinate.getLongitude())).collect(Collectors.toList())).collect(Collectors.toList());
											NEONSite neonSite = new NEONSite(rawNEONSite.getSiteName(), rawNEONSite.getSiteCode(), new Boundary(outerBoundary, innerBoundaries));
											// Set the NEON specific details
											neonSite.setDomainName(rawNEONSite.getDomainName());
											neonSite.setDomainCode(rawNEONSite.getDomainCode());
											neonSite.setSiteType(rawNEONSite.getSiteType());
											neonSite.setSiteDescription(rawNEONSite.getSiteDescription());
											neonSite.setStateName(rawNEONSite.getStateName());
											neonSite.setStateCode(rawNEONSite.getStateCode());
											// Add the neon site to our list
											neonSites.add(neonSite);
										}
									}
								}
							}
						}
						// We found the one entry we wanted, so no need to keep iterating
						break;
					}
				}
			}
		}

		return neonSites;
	}

	/**
	 * Retrieves the current KML file from the NEON site. NEON does not have an API for this, so we parse it manually
	 *
	 * @return The KML document representing the KML file
	 */
	private Kml getCurrentSiteKML()
	{
		try
		{
			// Open a connection to the NEON KMZ file URL
			URL neonKMZData = new URL(NEON_KMZ_LINK);
			// Create a temporary file to write the KMZ file to
			File neonKMZFile = CalliopeData.getInstance().getTempDirectoryManager().createTempFile("neon.kmz");
			// Write the KMZ url to a local file to store
			FileUtils.copyURLToFile(neonKMZData, neonKMZFile);
			// Open an input stream to the newly created file. We also open an archive stream because a KMZ file is just a
			// zip file with a KML inside
			try (InputStream fileInputStream = Files.newInputStream(neonKMZFile.toPath());
				 InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
				 ArchiveInputStream archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream))
			{
				ArchiveEntry archiveEntry;
				// Iterate over the archives inside of the KMZ file. There should be 2 files, one folder with images that we don't
				// need, and one kml file that contains the important information
				while ((archiveEntry = archiveInputStream.getNextEntry()) != null)
				{
					// Test to see if we've found the KML file
					if (!archiveEntry.isDirectory() && archiveEntry.getName().equals("doc.kml"))
					{
						// Read the raw KML file into a string
						String rawKml = IOUtils.toString(archiveInputStream, Charset.defaultCharset());
						// Parse the KML string into a structured KML format using a library and return it
						return Kml.unmarshal(rawKml);
					}
				}
			}
			catch (ArchiveException e)
			{
				// If the KMZ file is corrupt, it will throw an error here
				CalliopeData.getInstance().getErrorDisplay().notify("Error parsing KMZ zip file.\n" + ExceptionUtils.getStackTrace(e));
			}
		}
		catch (IOException e)
		{
			// If the KMZ file doesn't exist, print an error
			CalliopeData.getInstance().getErrorDisplay().notify("Error pulling and parsing KMZ file from the NEON server.\n" + ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	/**
	 * Given a latitude and a longitude this method returns the closest site
	 *
	 * @param rawNEONSites The sites to search through
	 * @param latitude The latitude to test
	 * @param longitude The longitude to test
	 * @return The site closest to the lat/long pair
	 */
	private RawNEONSite closestSiteTo(List<RawNEONSite> rawNEONSites, Double latitude, Double longitude)
	{
		// Compute the shortest distance, test each site
		Double shortestDistance = Double.MAX_VALUE;
		RawNEONSite closestRawNEONSite = null;
		// Iterate over all sites
		for (RawNEONSite rawNEONSite : rawNEONSites)
		{
			// Compute the distance between the site and the lat/long point
			Double distanceToSite = AnalysisUtils.distanceBetween(latitude, longitude, rawNEONSite.getSiteLatitude(), rawNEONSite.getSiteLongitude());
			// If this site is the closest so far, store it
			if (distanceToSite < shortestDistance)
			{
				// Store the site and the distance
				shortestDistance = distanceToSite;
				closestRawNEONSite = rawNEONSite;
			}
		}
		// Return the closest site
		return closestRawNEONSite;
	}

	/**
	 * Pulls the list of NEON sites from the NEON api and returns them in a structured format
	 */
	private List<RawNEONSite> pullSites()
	{
		try
		{
			// Setup the correct URL
			URL neonSiteAPI = new URL(NEON_API_URL + "/sites");
			// Establish a connection to the NEON site
			URLConnection neonSiteConnection = neonSiteAPI.openConnection();
			// Read the entire response into a buffered reader
			BufferedReader jsonReader = new BufferedReader(new InputStreamReader(neonSiteConnection.getInputStream()));
			// Join all the lines together into a single JSON string
			String json = jsonReader.lines().collect(Collectors.joining());
			// Convert the JSON string into a structured format
			RawNEONSite[] rawNEONSites = CalliopeData.getInstance().getGson().fromJson(json, RawNEONSiteList.class).getData();
			// Store the result
			return Arrays.asList(rawNEONSites);
		}
		catch (IOException e)
		{
			// If an error happened, print the message
			CalliopeData.getInstance().getErrorDisplay().notify("Could not retrieve NEON sites, error was:\n" + ExceptionUtils.getStackTrace(e));
		}
		return Collections.emptyList();
	}

	/**
	 * Prints out a KML file's feature as a tree
	 *
	 * @param feature The feature to print
	 * @param depth The current recursive depth
	 */
	private void printFeatureHierarchy(Feature feature, Integer depth)
	{
		// If it's a folder, print that out and then recurse into it
		if (feature instanceof Folder)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <folder> " + feature.getName());
			((Folder) feature).getFeature().forEach(featureNext -> this.printFeatureHierarchy(featureNext, depth + 1));
		}
		// If it's a document, print that out and then recurse into it
		else if (feature instanceof Document)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <document> " + feature.getName());
			Document document = (Document) feature;
			document.getFeature().forEach(featureNext -> this.printFeatureHierarchy(featureNext, depth + 1));
		}
		// If it's a placemark, print out the geometry type as well
		else if (feature instanceof Placemark)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <placemark> " + feature.getName());
			Geometry geometry = ((Placemark) feature).getGeometry();
			System.out.println(StringUtils.repeat("-", (depth + 1) * 2) + " " + geometry.getClass().getSimpleName());
		}
		// The rest of the types are not interesting to us, so just print them
		else if (feature instanceof GroundOverlay)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <ground_overlay> " + feature.getName());
		}
		else if (feature instanceof NetworkLink)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <network_link> " + feature.getName());
		}
		else if (feature instanceof PhotoOverlay)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <photo_overlay> " + feature.getName());
		}
		else if (feature instanceof ScreenOverlay)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <screen_overlay> " + feature.getName());
		}
		else if (feature instanceof Tour)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <tour> " + feature.getName());
		}
	}
}
