package model.elasticsearch.query.conditions;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.elasticsearch.query.ElasticSearchQuery;
import model.elasticsearch.query.QueryCondition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data model used by the "File Type filter" query condition
 */
public class FileTypeCondition extends QueryCondition
{
	// The currently recognized list of file types
	private ObservableList<String> fileTypes = FXCollections.observableArrayList();
	// A map of day of week -> if the day of week is selected to be filtered
	private Map<String, BooleanProperty> fileTypeToSelected = new HashMap<>();
	// A flag that gets set to true once we initialize the list of file types
	private Boolean wasInitializedOnce = false;

	/**
	 * Called when the current file type condition should be appended to the given query parameter
	 *
	 * @param query The query to append this file type condition to
	 */
	@Override
	public void appendConditionToQuery(ElasticSearchQuery query)
	{
		// For each file type, if it is selected, add it to the query
		for (String fileType : fileTypes)
			if (this.fileTypeToSelected.containsKey(fileType) && this.fileTypeToSelected.get(fileType).getValue())
				query.addFileType(fileType);
	}

	/**
	 * Called to update the internal model of the condition with new file types
	 *
	 * @param fileTypes The new file types to insert
	 */
	public void updateFileTypeList(List<String> fileTypes)
	{
		// Clear existing file types and selected boolean map
		this.fileTypes.clear();
		this.fileTypeToSelected.clear();

		// For each file type, add a new boolean flag that tells us if it's selected, Default to false
		fileTypes.forEach(fileType -> this.fileTypeToSelected.put(fileType, new SimpleBooleanProperty(false)));
		// Add all file types to the observable list
		this.fileTypes.addAll(fileTypes);
		// The data was now initialized at least once
		this.wasInitializedOnce = true;
	}

	/**
	 * Gets the property defining if a file type is selected
	 *
	 * @param fileType The file type to test if it's selected
	 * @return The property representing if the file type is selected
	 */
	public BooleanProperty fileTypeSelectedProperty(String fileType)
	{
		return this.fileTypeToSelected.get(fileType);
	}

	/**
	 * @return True if updateFileTypeList() was called at least once, false otherwise
	 */
	public Boolean wasInitializedOnce()
	{
		return this.wasInitializedOnce;
	}

	/**
	 * @return A string representing the FXML file that adds the UI front end editor to this condition
	 */
	@Override
	public String getFXMLConditionEditor()
	{
		return "FileTypeCondition.fxml";
	}

	/**
	 * @return Return the current file types list
	 */
	public ObservableList<String> getFileTypes()
	{
		return this.fileTypes;
	}
}
