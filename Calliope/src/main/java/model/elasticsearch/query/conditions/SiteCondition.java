package model.elasticsearch.query.conditions;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import model.CalliopeData;
import model.elasticsearch.query.ElasticSearchQuery;
import model.elasticsearch.query.QueryCondition;
import model.site.Site;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Data model class for the neon condition filter
 */
public class SiteCondition extends QueryCondition
{
	// A map of site -> if the site is selected to be filtered
	private Map<Site, BooleanProperty> siteToSelected = new HashMap<>();

	public SiteCondition()
	{
		// Make sure each site maps to a boolean property, this is important for later, since our view will use this to populate checkboxes
		for (Site site : this.getSites())
			if (!this.siteToSelected.containsKey(site))
				this.siteToSelected.put(site, new SimpleBooleanProperty(true));
		// If the collections list changes, we add a boolean property for the new added image collection
		this.getSites().addListener((ListChangeListener<Site>) c ->
		{
			while (c.next())
				if (c.wasAdded())
					for (Site site : c.getAddedSubList())
						if (!this.siteToSelected.containsKey(site))
							this.siteToSelected.put(site, new SimpleBooleanProperty(true));
		});
	}

	@Override
	public void appendConditionToQuery(ElasticSearchQuery query)
	{
		for (Site site : this.getSites())
			if (siteToSelected.containsKey(site) && siteToSelected.get(site).getValue())
				query.addSite(site);
	}

	@Override
	public String getFXMLConditionEditor()
	{
		return "SiteCondition.fxml";
	}

	/**
	 * Returns the list of sites
	 *
	 * @return A list of sites to filter
	 */
	public ObservableList<Site> getSites()
	{
		return CalliopeData.getInstance().getSiteManager().getSites();
	}

	/**
	 * Gets the property defining if a site is selected
	 *
	 * @param site The site to test if it's selected
	 * @return The property representing if the site is selected
	 */
	public BooleanProperty siteSelectedProperty(Site site)
	{
		if (!this.siteToSelected.containsKey(site))
			this.siteToSelected.put(site, new SimpleBooleanProperty(true));
		return this.siteToSelected.get(site);
	}

	/**
	 * Selects all image sites
	 */
	public void selectAll()
	{
		for (BooleanProperty selected : this.siteToSelected.values())
			selected.set(true);
	}

	/**
	 * De-selects all image sites
	 */
	public void selectNone()
	{
		for (BooleanProperty selected : this.siteToSelected.values())
			selected.set(false);
	}

	/**
	 * Selects images take at NEON sites only
	 */
	public void selectNEON()
	{
		this.siteToSelected.forEach((site, selected) ->
		{
			if (StringUtils.startsWithIgnoreCase(site.getCode(), "NEON"))
				selected.setValue(true);
			else
				selected.setValue(false);
		});
	}

	/**
	 * Selects images take at LTAR sites only
	 */
	public void selectLTAR()
	{
		this.siteToSelected.forEach((site, selected) ->
		{
			if (StringUtils.startsWithIgnoreCase(site.getCode(), "LTAR"))
				selected.setValue(true);
			else
				selected.setValue(false);
		});
	}
}
