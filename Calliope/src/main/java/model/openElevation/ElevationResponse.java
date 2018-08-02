package model.openElevation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to deserialize a JSON response from oepn elevation into an object
 */
public class ElevationResponse
{
	// The list of results
	private List<ElevationDataEntry> results = new ArrayList<>();

	/**
	 * @return The list of results
	 */
	public List<ElevationDataEntry> getResults()
	{
		return this.results;
	}
}
