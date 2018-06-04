package model.neon;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import model.SanimalData;
import model.neon.jsonPOJOs.ProductAvailability;
import model.neon.jsonPOJOs.Site;
import model.neon.jsonPOJOs.Sites;
import model.threading.ErrorTask;
import model.util.SanimalAnalysisUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.fxmisc.easybind.EasyBind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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

	public NeonData()
	{
	}

	public Site closestSiteTo(Double latitude, Double longitude)
	{
		Double shortestDistance = Double.MAX_VALUE;
		Site closestSite = null;
		for (Site site : this.sites)
		{
			Double distanceToSite = SanimalAnalysisUtils.distanceBetween(latitude, site.getSiteLatitude(), longitude, site.getSiteLongitude(), 0, 0);
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
		return SanimalData.getInstance().getGson().fromJson(json, Sites.class);
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
