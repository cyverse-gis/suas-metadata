package model.elasticsearch.query;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Base class for all query conditions
 */
public abstract class QueryCondition
{
	// A flag that tells us if this query condition is enabled or not
	private BooleanProperty enabled = new SimpleBooleanProperty(true);

	/**
	 * Called when the current condition should be appended to the given query parameter
	 *
	 * @param query The query to append this condition to
	 */
	public abstract void appendConditionToQuery(ElasticSearchQuery query);

	/**
	 * @return A string representing the FXML file that adds the UI front end editor to this condition
	 */
	public abstract String getFXMLConditionEditor();

	///
	/// Setters / Getters
	///

	public void setEnabled(boolean enabled)
	{
		this.enabled.setValue(enabled);
	}

	public boolean isEnabled()
	{
		return this.enabled.getValue();
	}

	public BooleanProperty enabledProperty()
	{
		return this.enabled;
	}
}
