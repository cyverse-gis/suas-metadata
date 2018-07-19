package model.elasticsearch.query;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public abstract class QueryCondition
{
	private BooleanProperty enabled = new SimpleBooleanProperty(true);

	public abstract void appendConditionToQuery(ElasticSearchQuery query);

	public abstract String getFXMLConditionEditor();

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
