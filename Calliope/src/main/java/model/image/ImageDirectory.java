package model.image;

import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import model.dataSources.IDataSource;
import model.site.Site;

import java.io.File;
import java.util.stream.Stream;

/**
 * A class representing a directoryProperty containing images
 * 
 * @author David Slovikosky
 */
public class ImageDirectory extends ImageContainer
{
	// The icon to use for all images at the moment
	private static final Image DEFAULT_DIR_IMAGE = new Image(ImageEntry.class.getResource("/images/importWindow/directoryIcon.png").toString());
	// The icon that is currently selected to be displayed
	private final ObjectProperty<Image> selectedImage = new SimpleObjectProperty<>(DEFAULT_DIR_IMAGE);
	// List of sub-files and directories
	private final ObservableList<ImageContainer> children = FXCollections.observableArrayList(imageContainer ->
	{
		if (imageContainer instanceof ImageEntry)
		{
			ImageEntry image = (ImageEntry) imageContainer;
			return new Observable[]
			{
				image.dateTakenProperty(),
				image.fileProperty(),
				image.positionTakenProperty(),
				image.treeIconProperty(),
				image.droneMakerProperty(),
				image.cameraModelProperty(),
				image.speedProperty(),
				image.rotationProperty()
			};
		}
		else if (imageContainer instanceof ImageDirectory)
		{
			ImageDirectory directory = (ImageDirectory) imageContainer;
			return new Observable[]
			{
				directory.getFileProperty(),
				// Do we need a ListProperty?
				directory.getChildren()
			};
		}
		else
			return new Observable[0];
	});
	// The file representing the directory
	private ObjectProperty<File> directoryProperty = new SimpleObjectProperty<File>();
	// The progress of the directory upload
	private DoubleProperty uploadProgress = new SimpleDoubleProperty(-1);
	// The source that provided this data
	private ObjectProperty<IDataSource> dataSource = new SimpleObjectProperty<>(null);

	/**
	 * Construct an image directoryProperty
	 * 
	 * @param directory
	 *            The file that represents the directoryProperty
	 */
	public ImageDirectory(File directory)
	{
		if (directory != null)
		{
			if (!directory.isDirectory())
				throw new RuntimeException("The specified file is not a directory!");
			this.directoryProperty.setValue(directory);
		}
	}

	/**
	 * Returns the image to represent this image directory
	 *
	 * @return An image representing this image directory
	 */
	@Override
	public ObjectProperty<Image> treeIconProperty()
	{
		return selectedImage;
	}

	/**
	 * Returns a list of children of this directory
	 *
	 * @return List of sub images and directories
	 */
	@Override
	public ObservableList<ImageContainer> getChildren()
	{
		return this.children;
	}

	/**
	 * Turns the recursive tree-like image directory format into a flat list of image containers
	 *
	 * @return A flat list of all image directories and image entries found in the recursive data structure
	 */
	public Stream<ImageContainer> flattened()
	{
		return Stream.concat(
			Stream.of(this),
			Stream.concat(
				this.getChildren()
					.stream()
					.filter(child -> !(child instanceof ImageDirectory)),
				this.getChildren()
					.stream()
					.filter(child -> child instanceof ImageDirectory)
					.map(child -> (ImageDirectory) child)
					.flatMap(ImageDirectory::flattened)));
	}

	/**
	 * Add a new child to this directory
	 *
	 * @param container The container to add
	 */
	public void addChild(ImageContainer container)
	{
		this.children.add(container);
	}

	/**
	 * Remove the container from the directory and all sub-directories
	 *
	 * @param container The container to remove
	 * @return True if the removal was successful
	 */
	public Boolean removeChildRecursive(ImageContainer container)
	{
		// If the children list successfully removes the item we return
		if (this.children.remove(container))
			return true;

		for (ImageContainer containerInList : this.children)
			if (containerInList instanceof ImageDirectory && ((ImageDirectory) containerInList).removeChildRecursive(container))
				return true;
		return false;
	}

	/**
	 * Get the file representing this directoryProperty
	 * 
	 * @return The file representing this directoryProperty
	 */
	public File getFile()
	{
		return directoryProperty.getValue();
	}

	/**
	 * Grab the file property
	 * @return The source file property
	 */
	public ObjectProperty<File> getFileProperty()
	{
		return this.directoryProperty;
	}

	/**
	 * Set the current progress from 0-1 of the upload, -1 meaning not uploading
	 *
	 * @param uploadProgress The new upload progress
	 */
	public void setUploadProgress(double uploadProgress)
	{
		this.uploadProgress.setValue(uploadProgress);
	}

	/**
	 * Get the current progress from 0-1 of the upload, -1 meaning not uploading
	 *
	 * @return The upload progress
	 */
	public double getUploadProgress()
	{
		return this.uploadProgress.getValue();
	}

	/**
	 * Get the current progress property
	 *
	 * @return The upload progress property
	 */
	public DoubleProperty uploadProgressProperty()
	{
		return this.uploadProgress;
	}

	/**
	 * Setting the site taken on a directory sets the site on all children recursively
	 *
	 * @param site The site to set to
	 */
	@Override
	public void setSiteTaken(Site site)
	{
		this.getChildren().forEach(child ->
		{
			// If our child is a currently uploading image directory we don't want to modify it, so ignore it
			if (child instanceof ImageDirectory && ((ImageDirectory) child).getUploadProgress() != -1)
				return;
			child.setSiteTaken(site);
		});
	}

	/**
	 * Setter for data source, also sets the data source for all child directories
	 *
	 * @param dataSource The data source of this directory
	 */
	public void setDataSource(IDataSource dataSource)
	{
		// Set the data source value
		this.dataSource.setValue(dataSource);
		// For each child, if it's a directory set its data source too
		for (ImageContainer containerInList : this.children)
			if (containerInList instanceof ImageDirectory)
				((ImageDirectory) containerInList).setDataSource(dataSource);
	}

	/**
	 * @return Returns the data source that this directory came from
	 */
	public IDataSource getDataSource()
	{
		return this.dataSource.getValue();
	}

	/**
	 * @return The data source property
	 */
	public ObjectProperty<IDataSource> dataSourceProperty()
	{
		return this.dataSource;
	}

	/**
	 * @return The string representing this directoryProperty
	 */
	@Override
	public String toString()
	{
		return this.getFile().getName();
	}
}
