package model.neon;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.CalliopeData;
import model.analysis.CalliopeAnalysisUtils;
import model.neon.jsonPOJOs.Site;
import model.neon.jsonPOJOs.Sites;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.stream.Collectors;

public class NeonData
{
	private static final String NEON_API_URL = "http://data.neonscience.org/api/v0";

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

	private UUID sessionID;

	public NeonData(UUID sessionID)
	{
		this.sessionID = sessionID;
	}

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

	public Sites pullSites() throws IOException
	{
		URL neonSiteAPI = new URL(NEON_API_URL + "/sites");
		URLConnection neonSiteConnection = neonSiteAPI.openConnection();
		BufferedReader jsonReader = new BufferedReader(new InputStreamReader(neonSiteConnection.getInputStream()));
		String json = jsonReader.lines().collect(Collectors.joining());
		return CalliopeData.getInstance().getGson().fromJson(json, Sites.class);
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
