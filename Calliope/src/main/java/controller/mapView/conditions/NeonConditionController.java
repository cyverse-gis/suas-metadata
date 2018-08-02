package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import model.elasticsearch.query.QueryCondition;
import model.elasticsearch.query.conditions.SiteCondition;
import model.site.Site;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

/**
 * Controller class for the NEON condition filter
 */
public class NeonConditionController implements IConditionController
{
	///
	/// FXML Bound Fields Start
	///

	// The listview of sites to filter
	@FXML
	public ListView<Site> siteFilterListView;
	// The search bar for the sites
	@FXML
	public TextField txtSiteSearch;

	///
	/// FXML Bound Fields End
	///

	// The data model reference
	private SiteCondition siteCondition;

	@Override
	public void initializeData(QueryCondition queryCondition)
	{
		// Make sure the data model we received matches our type
		if (queryCondition instanceof SiteCondition)
		{
			this.siteCondition = (SiteCondition) queryCondition;

			// Grab the site list from our data model item
			SortedList<Site> sitesSorted = new SortedList<>(this.siteCondition.getSites());
			// We set the comparator to be the name of the collection
			sitesSorted.setComparator(Comparator.comparing(Site::getName));
			// We create a local wrapper of the sites list to filter
			FilteredList<Site> sitesFiltered = new FilteredList<>(sitesSorted);
			// Set the filter to update whenever the search text changes
			this.txtSiteSearch.textProperty().addListener(observable -> {
				sitesFiltered.setPredicate(site ->
						// Allow any sites with a name or code containing the search text
						StringUtils.containsIgnoreCase(site.getName(), this.txtSiteSearch.getCharacters()) ||
						StringUtils.containsIgnoreCase(site.getCode(), this.txtSiteSearch.getCharacters()));
			});
			// Set the items of the sites list view to the newly sorted list
			this.siteFilterListView.setItems(sitesFiltered);
			// Each site gets a checkbox
			this.siteFilterListView.setCellFactory(CheckBoxListCell.forListView(site -> this.siteCondition.siteSelectedProperty(site)));
			this.siteFilterListView.setEditable(true);
		}
	}

	/**
	 * Clear the search bar when we click the clear search button
	 *
	 * @param actionEvent consumed
	 */
	public void clearSiteSearch(ActionEvent actionEvent)
	{
		this.txtSiteSearch.clear();
		actionEvent.consume();
	}

	/**
	 * Selects all sites for the given data model
	 *
	 * @param actionEvent consumed
	 */
	public void selectAllSites(ActionEvent actionEvent)
	{
		if (siteCondition != null)
			siteCondition.selectAll();
		actionEvent.consume();
	}

	/**
	 * Selects no sites for the given data model
	 *
	 * @param actionEvent consumed
	 */
	public void selectNoSites(ActionEvent actionEvent)
	{
		if (siteCondition != null)
			siteCondition.selectNone();
		actionEvent.consume();
	}
}