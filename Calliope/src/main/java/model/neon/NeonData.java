package model.neon;

import controller.Calliope;
import de.micromata.opengis.kml.v_2_2_0.*;
import de.micromata.opengis.kml.v_2_2_0.gx.Tour;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.CalliopeData;
import model.analysis.CalliopeAnalysisUtils;
import model.neon.jsonPOJOs.Site;
import model.neon.jsonPOJOs.Sites;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NeonData
{
	private static final String NEON_API_URL = "http://data.neonscience.org/api/v0";
	private static final String NEON_KMZ_LINK = "https://www.neonscience.org/sites/default/files/NEON-Project-Locations-20180605v16.kmz";

	private ObservableList<Site> sites = FXCollections.observableArrayList(site -> new Observable[]
	{
			site.domainCodeProperty(),
			site.domainNameProperty(),
			site.siteCodeProperty(),
			site.siteDescriptionProperty(),
			site.siteLatitudeProperty(),
			site.siteLongitudeProperty(),
			site.siteNameProperty(),
			site.siteTypeProperty(),
			site.stateCodeProperty(),
			site.stateNameProperty(),
			site.dataProductsProperty()
	});

	public Site closestSiteTo(Double latitude, Double longitude)
	{
		Double shortestDistance = Double.MAX_VALUE;
		Site closestSite = null;
		for (Site site : this.sites)
		{
			Double distanceToSite = CalliopeAnalysisUtils.distanceBetween(latitude, site.getSiteLatitude(), longitude, site.getSiteLongitude());
			if (distanceToSite < shortestDistance)
			{
				shortestDistance = distanceToSite;
				closestSite = site;
			}
		}
		return closestSite;
	}

	public void pullAndStoreSites()
	{
		try
		{
			URL neonSiteAPI = new URL(NEON_API_URL + "/sites");
			URLConnection neonSiteConnection = neonSiteAPI.openConnection();
			BufferedReader jsonReader = new BufferedReader(new InputStreamReader(neonSiteConnection.getInputStream()));
			String json = jsonReader.lines().collect(Collectors.joining());
			Site[] sites = CalliopeData.getInstance().getGson().fromJson(json, Sites.class).getData();
			Platform.runLater(() -> this.sites.setAll(sites));
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Could not retrieve NEON sites, error was:\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	public Kml getCurrentSiteKML()
	{
		try
		{
			URL neonKMZData = new URL(NEON_KMZ_LINK);
			File neonKMZFile = CalliopeData.getInstance().getTempDirectoryManager().createTempFile("neon.kmz");
			FileUtils.copyURLToFile(neonKMZData, neonKMZFile);
			try (InputStream fileInputStream = Files.newInputStream(neonKMZFile.toPath());
				 InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
				 ArchiveInputStream archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream))
			{
				ArchiveEntry archiveEntry;
				while ((archiveEntry = archiveInputStream.getNextEntry()) != null)
				{
					if (!archiveEntry.isDirectory() && archiveEntry.getName().equals("doc.kml"))
					{
						String rawKml = IOUtils.toString(archiveInputStream, Charset.defaultCharset());
						return Kml.unmarshal(rawKml);
						//Feature feature = kml.getFeature();
						//this.printFeatureHierarchy(feature, 0);
						//break;
					}
				}
			}
			catch (ArchiveException e)
			{
				CalliopeData.getInstance().getErrorDisplay().notify("Error parsing KMZ zip file.\n" + ExceptionUtils.getStackTrace(e));
			}
		}
		catch (IOException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Error pulling and parsing KMZ file from the NEON server.\n" + ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	public List<BoundedSite> parseBoundedSites(Kml kml)
	{
		List<BoundedSite> boundedSites = new ArrayList<>();

		Feature lvl0Feature = kml.getFeature();
		if (lvl0Feature.getName().equals("NEON Project Locations (v16)") && lvl0Feature instanceof Folder)
		{
			Folder topFolder = (Folder) lvl0Feature;
			for (Feature lvl1Feature : topFolder.getFeature())
			{
				if (lvl1Feature.getName().equals("NEON_Field_Sampling_Boundaries") && lvl1Feature instanceof Document)
				{
					Document fieldSamplingBoundaries = (Document) lvl1Feature;
					Feature lvl2FeaturePoly = fieldSamplingBoundaries.getFeature().stream().filter(lvl2Feature -> lvl2Feature.getName().equals("Features")).findFirst().get();
					Feature lvl2FeatureLabl = fieldSamplingBoundaries.getFeature().stream().filter(lvl2Feature -> lvl2Feature.getName().equals("Feature Labels (Name)")).findFirst().get();
					if (lvl2FeaturePoly instanceof Folder && lvl2FeatureLabl instanceof Folder)
					{
						List<Feature> polygons = ((Folder) lvl2FeaturePoly).getFeature();
						List<Feature> labels = ((Folder) lvl2FeatureLabl).getFeature();
						if (polygons.size() == labels.size() && polygons.stream().allMatch(feature -> feature instanceof Placemark) && labels.stream().allMatch(label -> label instanceof Placemark))
						{
							for (Integer i = 0; i < polygons.size(); i++)
							{
								Placemark polygonPlacemark = (Placemark) polygons.get(i);
								Placemark labelPlacemark = (Placemark) labels.get(i);
								Geometry polygonRaw = polygonPlacemark.getGeometry();
								Geometry labelRaw = labelPlacemark.getGeometry();
								if (polygonRaw instanceof Polygon && labelRaw instanceof Point)
								{
									Polygon boundary = (Polygon) polygonRaw;
									Point location = (Point) labelRaw;
									Coordinate locationCoord = location.getCoordinates().get(0);
									Site site = this.closestSiteTo(locationCoord.getLatitude(), locationCoord.getLongitude());
									if (site != null)
									{
										boundedSites.add(new BoundedSite(site, boundary));
									}
								}
							}
						}
					}
				}
				break;
			}
		}

		return boundedSites;
	}

	private void parseFeature(Feature feature)
	{
		if (feature instanceof Folder)
		{
			((Folder) feature).getFeature().forEach(this::parseFeature);
		}
		else if (feature instanceof Document)
		{
			Document document = (Document) feature;
			document.getFeature().forEach(this::parseFeature);
		}
		else if (feature instanceof Placemark)
		{
			Geometry geometry = ((Placemark) feature).getGeometry();
		}
		else if (feature instanceof GroundOverlay)
		{
		}
		else if (feature instanceof NetworkLink)
		{
		}
		else if (feature instanceof PhotoOverlay)
		{
		}
		else if (feature instanceof ScreenOverlay)
		{
		}
		else if (feature instanceof Tour)
		{
		}
	}

	private void printFeatureHierarchy(Feature feature, Integer depth)
	{
		if (feature instanceof Folder)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <folder> " + feature.getName());
			((Folder) feature).getFeature().forEach(featureNext -> this.printFeatureHierarchy(featureNext, depth + 1));
		}
		else if (feature instanceof Document)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <document> " + feature.getName());
			Document document = (Document) feature;
			document.getFeature().forEach(featureNext -> this.printFeatureHierarchy(featureNext, depth + 1));
		}
		else if (feature instanceof Placemark)
		{
			System.out.println(StringUtils.repeat("-", depth * 2) + " <placemark> " + feature.getName());
			Geometry geometry = ((Placemark) feature).getGeometry();
			System.out.println(StringUtils.repeat("-", (depth + 1) * 2) + " " + geometry.getClass().getSimpleName());
		}
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

	public void setSites(Sites sites)
	{
		this.sites.setAll(sites.getData());
	}

	public ObservableList<Site> getSites()
	{
		return this.sites;
	}
}
