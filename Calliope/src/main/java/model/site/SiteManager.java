package model.site;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import model.site.ltar.LTARData;
import model.site.neon.NeonData;
import model.util.AnalysisUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class representing a site manager which maintains a list of sites from various sources
 */
public class SiteManager
{
	// A global list of sites
	private final ObservableList<Site> siteList;

	// A map of site code -> site for fast search
	private final Map<String, Site> codeToSite;

	// A neon data source that is used to download neon data
	private final NeonData neonData = new NeonData();

	// A ltar data source that is used to download ltar data
	private final LTARData ltarData = new LTARData();

	/**
	 * Constructor initializes the site list
	 */
	public SiteManager()
	{
		// Create the location list and add some default locations
		this.siteList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(site -> new Observable[]{ site.nameProperty(), site.boundaryProperty(), site.centerProperty(), site.codeProperty() }));
		// A map of site code -> site for accelerated site lookup
		this.codeToSite = new HashMap<>();
		// When the site list changes we update our site mapping
		this.siteList.addListener((ListChangeListener<Site>) c ->
		{
			while (c.next())
				// If a new site was added, add the code -> site mapping
				if (c.wasAdded())
				{
					for (Site site : c.getAddedSubList())
						if (!this.codeToSite.containsKey(site.getCode()))
							this.codeToSite.put(site.getCode(), site);
				}
				// If a new site was removed, add the code -> site mapping
				else if (c.wasRemoved())
				{
					for (Site site : c.getRemoved())
						this.codeToSite.remove(site.getCode());
				}
		});
	}

	/**
	 * Given a latitude and a longitude this method returns the closest site
	 *
	 * @param sites The sites to search through
	 * @param latitude The latitude to test
	 * @param longitude The longitude to test
	 * @return The site closest to the lat/long pair
	 */
	public Site closestSiteTo(List<Site> sites, Double latitude, Double longitude)
	{
		// Compute the shortest distance, test each site
		Double shortestDistance = Double.MAX_VALUE;
		Site closestSite = null;
		// Iterate over all sites
		for (Site site : sites)
		{
			// Compute the distance between the site and the lat/long point
			Double distanceToSite = AnalysisUtils.distanceBetween(latitude, longitude, site.getCenter().getLat(), site.getCenter().getLon());
			// If this site is the closest so far, store it
			if (distanceToSite < shortestDistance)
			{
				// Store the site and the distance
				shortestDistance = distanceToSite;
				closestSite = site;
			}
		}
		// Return the closest site
		return closestSite;
	}

	/**
	 * Download sites downloads all sites from all sources and returns them as a list
	 *
	 * @return A list of sites that were downloaded
	 */
	public List<? extends Site> downloadSites()
	{
		return Stream.concat(this.neonData.retrieveSites().stream(), this.ltarData.retrieveSites().stream()).collect(Collectors.toList());
	}

	/**
	 * Getter for currently stored list of sites
	 *
	 * @return The list of sites
	 */
	public ObservableList<Site> getSites()
	{
		return this.siteList;
	}

	public Site getSiteByCode(String siteCode)
	{
		return this.codeToSite.get(siteCode);
	}
}
