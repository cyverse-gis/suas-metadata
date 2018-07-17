package model.settings;

import javafx.beans.value.ObservableValue;
import model.util.CustomPropertyItem;

import java.util.Optional;

public class MetadataCustomItem extends CustomPropertyItem<String>
{
	private String value;

	/**
	 * Constructor used to initialize all fields
	 *
	 * @param name        The name of the property
	 * @param value       The value of the property
	 */
	public MetadataCustomItem(String name, String value)
	{
		super(name, "Metadata", "", null, String.class);
		this.value = value;
	}

	@Override
	public Object getValue()
	{
		return this.value;
	}

	@Override
	public void setValue(Object value)
	{
		this.value = value.toString();
	}

	@Override
	public Optional<ObservableValue<?>> getObservableValue()
	{
		return Optional.empty();
	}
}
