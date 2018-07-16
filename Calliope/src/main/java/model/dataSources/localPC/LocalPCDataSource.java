package model.dataSources.localPC;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.image.Image;
import model.dataSources.IDataSource;

/**
 * Data source used by the local PC
 */
public abstract class LocalPCDataSource implements IDataSource
{
	// The name of the data source
	private ReadOnlyStringWrapper name = new ReadOnlyStringWrapper();
	// The description of the data source
	private ReadOnlyStringWrapper description = new ReadOnlyStringWrapper();
	// The image representing this data source
	private ReadOnlyObjectWrapper<Image> image = new ReadOnlyObjectWrapper<>();

	/**
	 * Constructor sets the name, description, and image of the data source
	 *
	 * @param name The name of the data source
	 * @param description The description of the data source
	 * @param image The image of the data source
	 */
	public LocalPCDataSource(String name, String description, Image image)
	{
		this.name.setValue(name);
		this.description.setValue(description);
		this.image.setValue(image);
	}

	///
	/// Getters
	///

	@Override
	public String getName()
	{
		return this.name.getValue();
	}

	@Override
	public ReadOnlyStringProperty nameProperty()
	{
		return this.name.getReadOnlyProperty();
	}

	@Override
	public String getDescription()
	{
		return this.description.getValue();
	}

	@Override
	public ReadOnlyStringProperty descriptionProperty()
	{
		return this.description.getReadOnlyProperty();
	}

	@Override
	public Image getIcon()
	{
		return this.image.getValue();
	}

	@Override
	public ReadOnlyObjectProperty<Image> iconProperty()
	{
		return this.image.getReadOnlyProperty();
	}
}
