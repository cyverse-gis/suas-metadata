package model.image;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.util.Pair;
import model.SanimalData;
import model.constant.SanimalMetadataFields;
import model.location.Location;
import model.species.Species;
import model.species.SpeciesEntry;
import model.util.MetadataUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.domain.AvuData;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


/**
 * A class representing an image file
 * 
 * @author David Slovikosky
 */
public class ImageEntry extends ImageContainer
{
	private static final DateTimeFormatter DATE_FORMAT_FOR_DISK = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

	// The icon to use for all images at the moment
	private static final Image DEFAULT_IMAGE_ICON = new Image(ImageEntry.class.getResource("/images/importWindow/imageIcon.png").toString());
	// The icon to use for all tagged images at the moment
	private static final Image CHECKED_IMAGE_ICON = new Image(ImageEntry.class.getResource("/images/importWindow/imageIconDone.png").toString());

	// A property to wrap the currently selected image property. Must not be static!
	transient final ObjectProperty<Image> selectedImageProperty = new SimpleObjectProperty<>(DEFAULT_IMAGE_ICON);
	// The actual file 
	private final ObjectProperty<File> imageFileProperty = new SimpleObjectProperty<File>();
	private final ObservableList<MetadataEntry> imageMetadata = FXCollections.observableArrayList(metadataEntry -> new Observable[] { metadataEntry.tagProperty(), metadataEntry.valueProperty() });
	// If this image is dirty, we set a flag to write it to disk at some later point
	private transient final AtomicBoolean isDiskDirty = new AtomicBoolean(false);

	/**
	 * Create a new image entry with an image file
	 * 
	 * @param file
	 *            The file (must be an image file)
	 */
	public ImageEntry(File file)
	{
		this.imageFileProperty.setValue(file);

	}

	/**
	 * Reads the file metadata and initializes fields
	 */
	public void readFileMetadataIntoImage()
	{
		try
		{
			//Read the metadata off of the image
			Map<Tag, String> imageMetadataMap = MetadataUtils.readImageMetadata(this.getFile());

			imageMetadataMap.forEach((tag, value) -> this.imageMetadata.add(new MetadataEntry(tag, value)));

			this.markDiskDirty(false);
		}
		catch (Exception e)
		{
			SanimalData.getInstance().getErrorDisplay().showPopup(
					Alert.AlertType.ERROR,
					null,
					"Error",
					"Metadata error",
					"Error reading image metadata for file " + this.getFile().getName() + "!\n" + ExceptionUtils.getStackTrace(e),
					false);
		}
	}

	/**
	 * Used to initialize icon bindings to their default
	 */
	public void initIconBindings()
	{
		// Bind the image property to a conditional expression.
		// The image is checked if the location is valid and the species present list is not empty
		Binding<Image> imageBinding = Bindings.createObjectBinding(() ->
		{
			return DEFAULT_IMAGE_ICON;
		});
		selectedImageProperty.bind(imageBinding);
	}

	/**
	 * Writes the image entry's metadata to CyVerse's AVU format
	 *
	 * @return A list of AVU entries representing the metadata
	 * @throws JargonException If anything goes wrong
	 */
	public List<AvuData> convertToAVUMetadata() throws JargonException
	{
		// Create a list to return
		List<AvuData> metadata = new LinkedList<>();

		/*

		// Grab the location taken
		Location locationTaken = this.getLocationTaken();

		// Flag saying this image was tagged by sanimal
		metadata.add(AvuData.instance(SanimalMetadataFields.A_SANIMAL, "true", ""));

		// Metadata of the image's date taken
		metadata.add(AvuData.instance(SanimalMetadataFields.A_DATE_TIME_TAKEN, Long.toString(this.getDateTaken().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), ""));
		metadata.add(AvuData.instance(SanimalMetadataFields.A_DATE_YEAR_TAKEN, Integer.toString(this.getDateTaken().getYear()), ""));
		metadata.add(AvuData.instance(SanimalMetadataFields.A_DATE_MONTH_TAKEN, Integer.toString(this.getDateTaken().getMonthValue()), ""));
		metadata.add(AvuData.instance(SanimalMetadataFields.A_DATE_HOUR_TAKEN, Integer.toString(this.getDateTaken().getHour()), ""));
		metadata.add(AvuData.instance(SanimalMetadataFields.A_DATE_DAY_OF_YEAR_TAKEN, Integer.toString(this.getDateTaken().getDayOfYear()), ""));
		metadata.add(AvuData.instance(SanimalMetadataFields.A_DATE_DAY_OF_WEEK_TAKEN, Integer.toString(this.getDateTaken().getDayOfWeek().getValue()), ""));

		// Location metadata
		metadata.add(AvuData.instance(SanimalMetadataFields.A_LOCATION_NAME, locationTaken.getName(), ""));
		metadata.add(AvuData.instance(SanimalMetadataFields.A_LOCATION_ID, locationTaken.getId(), ""));
		metadata.add(AvuData.instance(SanimalMetadataFields.A_LOCATION_LATITUDE, locationTaken.getLat().toString(), ""));
		metadata.add(AvuData.instance(SanimalMetadataFields.A_LOCATION_LONGITUDE, locationTaken.getLng().toString(), ""));
		metadata.add(AvuData.instance(SanimalMetadataFields.A_LOCATION_ELEVATION, locationTaken.getElevation().toString(), "meters"));

		// Species metadata uses the AVU Unit as a foreign key to link a list of entries together
		Integer entryID = 0;
		for (SpeciesEntry speciesEntry : this.getSpeciesPresent())
		{
			metadata.add(AvuData.instance(SanimalMetadataFields.A_SPECIES_SCIENTIFIC_NAME, speciesEntry.getSpecies().getScientificName(), entryID.toString()));
			metadata.add(AvuData.instance(SanimalMetadataFields.A_SPECIES_COMMON_NAME, speciesEntry.getSpecies().getName(), entryID.toString()));
			metadata.add(AvuData.instance(SanimalMetadataFields.A_SPECIES_COUNT, speciesEntry.getAmount().toString(), entryID.toString()));
			entryID++;
		}
		*/

		return metadata;
	}

	/**
	 * Getter for the tree icon property
	 *
	 * @return The tree icon to be used
	 */
	@Override
	public ObjectProperty<Image> getTreeIconProperty()
	{
		return selectedImageProperty;
	}

	/**
	 * Get the image file
	 * 
	 * @return The image file
	 */
	public File getFile()
	{
		return this.imageFileProperty.getValue();
	}

	/**
	 * Get the image file property that this image represents
	 *
	 * @return The file property that this image represents
	 */
	public ObjectProperty<File> getFileProperty()
	{
		return this.imageFileProperty;
	}

	public ObservableList<MetadataEntry> getImageMetadata()
	{
		return imageMetadata;
	}

	public <T> T getSpecificMetadataField(Tag tag)
	{
		for (MetadataEntry entry : this.getImageMetadata())
			if (entry.getTag().equals(tag))
				return tag.parse(entry.getValue());
		return null;
	}

	public void markDiskDirty(Boolean dirty)
	{
		this.isDiskDirty.set(dirty);
	}

	public Boolean isDiskDirty()
	{
		return this.isDiskDirty.get();
	}

	/**
	 * Writes the species and location tagged in this image to the disk
	 */
	public synchronized void writeToDisk()
	{

	}
}
