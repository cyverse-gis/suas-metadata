package controller.mapView.conditions;

import controller.mapView.IConditionController;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import model.elasticsearch.query.IQueryCondition;
import model.elasticsearch.query.conditions.NeonCondition;
import model.neon.BoundedSite;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.Comparator;
import java.util.ResourceBundle;

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
	public ListView<BoundedSite> siteFilterListView;
	// The search bar for the sites
	@FXML
	public TextField txtSiteSearch;

	///
	/// FXML Bound Fields End
	///

	// The data model reference
	private NeonCondition siteCondition;

	/**
	 * Initializes the UI, does nothing
	 *
	 * @param location  ignored
	 * @param resources ignored
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
	}

	@Override
	public void initializeData(IQueryCondition iQueryCondition)
	{
		// Make sure the data model we received matches our type
		if (iQueryCondition instanceof NeonCondition)
		{
			this.siteCondition = (NeonCondition) iQueryCondition;

			// Grab the site list from our data model item
			SortedList<BoundedSite> boundedSitesSorted = new SortedList<>(this.siteCondition.getBoundedSites());
			// We set the comparator to be the name of the collection
			boundedSitesSorted.setComparator(Comparator.comparing(boundedSite -> boundedSite.getSite().getSiteName()));
			// We create a local wrapper of the bounded sites list to filter
			FilteredList<BoundedSite> boundedSitesFilteredList = new FilteredList<>(boundedSitesSorted);
			// Set the filter to update whenever the search text changes
			this.txtSiteSearch.textProperty().addListener(observable -> {
				boundedSitesFilteredList.setPredicate(boundedSite ->
						// Allow any bounded sites with a name or code containing the search text
						StringUtils.containsIgnoreCase(boundedSite.getSite().getSiteName(), this.txtSiteSearch.getCharacters()) ||
								StringUtils.containsIgnoreCase(boundedSite.getSite().getSiteCode(), this.txtSiteSearch.getCharacters()));
			});
			// Set the items of the sites list view to the newly sorted list
			this.siteFilterListView.setItems(boundedSitesFilteredList);
			// Each site gets a checkbox
			this.siteFilterListView.setCellFactory(CheckBoxListCell.forListView(boundedSite -> this.siteCondition.boundedSiteSelectedProperty(boundedSite)));
			this.siteFilterListView.setEditable(true);
		}
	}

	/**
	 * Clear the search bar when we click the clear search button
	 *
	 * @param actionEvent consumed
	 */
	public void clearBoundedSiteSearch(ActionEvent actionEvent)
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