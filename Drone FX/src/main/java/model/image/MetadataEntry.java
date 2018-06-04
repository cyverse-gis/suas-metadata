package model.image;

import com.thebuzzmedia.exiftool.Tag;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MetadataEntry
{
	private final ObjectProperty<Tag> tag = new SimpleObjectProperty<>(null);
	private final StringProperty value = new SimpleStringProperty("");

	public MetadataEntry(Tag tag, String value)
	{
		this.tag.setValue(tag);
		this.value.setValue(value);
	}

	public void setValue(String value)
	{
		this.value.setValue(value);
	}

	public String getValue()
	{
		return this.value.getValue();
	}

	public StringProperty valueProperty()
	{
		return this.value;
	}

	public void setTag(Tag tag)
	{
		this.tag.setValue(tag);
	}

	public Tag getTag()
	{
		return this.tag.getValue();
	}

	public ObjectProperty<Tag> tagProperty()
	{
		return this.tag;
	}

	@Override
	public String toString()
	{
		return "Metadata: " + this.tag.getValue().toString() + " -> " + this.value.getValue();
	}
}
