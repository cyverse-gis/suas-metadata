package model.elasticsearch;

import com.thebuzzmedia.exiftool.Tag;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import model.cyverse.ImageCollection;
import model.image.ImageEntry;
import model.settings.MetadataCustomItem;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * A utility class which is an image entry but is just a container with simple metadata about an image.
 */
public class QueryImageEntry extends ImageEntry
{
	// Two additional properties, the collection this image belongs to and the absolute path of the image file
	private ObjectProperty<ImageCollection> imageCollection = new SimpleObjectProperty<>();
	private StringProperty irodsAbsolutePath = new SimpleStringProperty();

	/**
	 * Create a new query image entry without any information
	 */
	public QueryImageEntry()
	{
		super(null);
	}

	///
	/// Override any processing methods to throw exceptions because we can't process metadata without a file
	///

	@Override
	public void readFileMetadataFromImage()
	{
		throw new UnsupportedOperationException("You can't read metadata from a query image entry result");
	}
	@Override
	protected void readFileMetadataFromMap(Map<Tag, String> imageMetadataMap)
	{
		throw new UnsupportedOperationException("You can't read metadata from a query image entry result");
	}
	@Override
	protected BufferedImage retrieveRawImage()
	{
		throw new UnsupportedOperationException("You can't retrieve the raw image file from a query image entry result");
	}
	@Override
	public File getFile()
	{
		throw new UnsupportedOperationException("You can't retrieve the image file from a query image entry result");
	}
	@Override
	public ObjectProperty<File> fileProperty()
	{
		throw new UnsupportedOperationException("You can't retrieve the image file from a query image entry result");
	}
	@Override
	public Image buildIntoImage()
	{
		throw new UnsupportedOperationException("You can't retrieve the image file from a query image entry result");
	}
	@Override
	public void buildAndStoreIcon()
	{
		throw new UnsupportedOperationException("You can't retrieve the image file from a query image entry result");
	}
	@Override
	public ObjectProperty<Image> treeIconProperty()
	{
		throw new UnsupportedOperationException("A query image entry result does not have a tree icon");
	}
	@Override
	public List<MetadataCustomItem> getRawMetadata()
	{
		throw new UnsupportedOperationException("A query image entry result does not have associated raw metadata");
	}
	@Override
	public boolean isMetadataEditable()
	{
		throw new UnsupportedOperationException("A query image entry result does not have editable metadata");
	}
	@Override
	public BooleanProperty metadataEditableProperty()
	{
		throw new UnsupportedOperationException("A query image entry result does not have editable metadata");
	}

	///
	/// Getters & Setters of our properties
	///

	public void setImageCollection(ImageCollection imageCollection)
	{
		this.imageCollection.setValue(imageCollection);
	}
	public ImageCollection getImageCollection()
	{
		return this.imageCollection.getValue();
	}
	public ObjectProperty<ImageCollection> imageCollectionProperty()
	{
		return this.imageCollection;
	}

	public void setIrodsAbsolutePath(String irodsAbsolutePath)
	{
		this.irodsAbsolutePath.setValue(irodsAbsolutePath);
	}
	public String getIrodsAbsolutePath()
	{
		return this.irodsAbsolutePath.getValue();
	}
	public StringProperty irodsAbsolutePathProperty()
	{
		return this.irodsAbsolutePath;
	}

	/**
	 * @return Instead of printing out the file's name, we print out the irods absolute path since we don't have a file
	 */
	@Override
	public String toString()
	{
		return this.getIrodsAbsolutePath();
	}
}
