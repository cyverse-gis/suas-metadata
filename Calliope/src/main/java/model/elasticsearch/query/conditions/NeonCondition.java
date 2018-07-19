package model.elasticsearch.query.conditions;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import model.CalliopeData;
import model.elasticsearch.query.ElasticSearchQuery;
import model.elasticsearch.query.QueryCondition;
import model.neon.BoundedSite;

import java.util.HashMap;
import java.util.Map;

/**
 * Data model class for the neon condition filter
 */
public class NeonCondition extends QueryCondition
{
	// A map of site -> if the site is selected to be filtered
	private Map<BoundedSite, BooleanProperty> boundedSiteToSelected = new HashMap<>();

	public NeonCondition()
	{
		// Make sure each bounded site maps to a boolean property, this is important for later, since our view will use this to populate checkboxes
		for (BoundedSite boundedSite : this.getBoundedSites())
			if (!this.boundedSiteToSelected.containsKey(boundedSite))
				this.boundedSiteToSelected.put(boundedSite, new SimpleBooleanProperty(true));
		// If the collections list changes, we add a boolean property for the new added image collection
		this.getBoundedSites().addListener((ListChangeListener<BoundedSite>) c ->
		{
			while (c.next())
				if (c.wasAdded())
					for (BoundedSite boundedSite : c.getAddedSubList())
						if (!this.boundedSiteToSelected.containsKey(boundedSite))
							this.boundedSiteToSelected.put(boundedSite, new SimpleBooleanProperty(true));
		});
	}

	@Override
	public void appendConditionToQuery(ElasticSearchQuery query)
	{
		for (BoundedSite boundedSite : this.getBoundedSites())
			if (boundedSiteToSelected.containsKey(boundedSite) && boundedSiteToSelected.get(boundedSite).getValue())
				query.addNeonSite(boundedSite);
	}

	@Override
	public String getFXMLConditionEditor()
	{
		return "NeonCondition.fxml";
	}

	/**
	 * Returns the list of bounded sites
	 *
	 * @return A list of bounded sites to filter
	 */
	public ObservableList<BoundedSite> getBoundedSites()
	{
		return CalliopeData.getInstance().getSiteList();
	}

	/**
	 * Gets the property defining if a bounded site is selected
	 *
	 * @param boundedSite The bounded site to test if it's selected
	 * @return The property representing if the bounded site is selected
	 */
	public BooleanProperty boundedSiteSelectedProperty(BoundedSite boundedSite)
	{
		if (!this.boundedSiteToSelected.containsKey(boundedSite))
			this.boundedSiteToSelected.put(boundedSite, new SimpleBooleanProperty(true));
		return this.boundedSiteToSelected.get(boundedSite);
	}

	/**
	 * Selects all image sites
	 */
	public void selectAll()
	{
		for (BooleanProperty selected : this.boundedSiteToSelected.values())
			selected.set(true);
	}

	/**
	 * De-selects all image sites
	 */
	public void selectNone()
	{
		for (BooleanProperty selected : this.boundedSiteToSelected.values())
			selected.set(false);
	}
}
