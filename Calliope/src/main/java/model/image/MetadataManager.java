package model.image;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import model.util.ErrorDisplay;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Class containing utils for writing & reading metadata
 */
public class MetadataManager
{
	// A reference to the EXIF tool object used to read and write metadata
	private ExifTool exifTool;
	// Flag that is used in testing if we have found exiftool or not
	private final ReadOnlyBooleanWrapper exifToolFound = new ReadOnlyBooleanWrapper(false);

	/**
	 * Metadata Manager constructor just starts the exif tool process
	 */
	public MetadataManager(ErrorDisplay errorDisplay)
	{
		// If we're on windows, we can use our own exiftool.exe executable
		if (SystemUtils.IS_OS_WINDOWS)
		{
			// Create our exiftool file next the the jar file
			File exiftoolFile = new File("./exiftool.exe");
			// If the exiftool file already exists, just use that one
			if (!exiftoolFile.exists())
			{
				try
				{
					// Create a new exiftool executable
					exiftoolFile.createNewFile();
					// Copy our original exiftool copy fron side of the JAR to the outside file ready to be executed
					InputStream inputStream = MetadataManager.class.getResource("/files/exiftool.exe").openStream();
					FileOutputStream outputStream = new FileOutputStream(exiftoolFile);
					IOUtils.copy(inputStream, outputStream);
					// Close the streams
					inputStream.close();
					outputStream.close();
				}
				catch (IOException e)
				{
					// If the copy fails show an error
					errorDisplay.notify("Error copying exiftool from jar to temporary directory!\n" + ExceptionUtils.getStackTrace(e));
				}
			}
			// Open a connection to the exiftool file
			this.exifTool = new ExifToolBuilder().withPath(exiftoolFile).enableStayOpen().build();
			this.exifToolFound.setValue(true);
		}
		// Otherwise we test the path, if exiftool is in our path use that one
		else if (System.getProperty("exiftool.path", "exiftool") != null)
		{
			this.exifTool = new ExifToolBuilder().enableStayOpen().build();
			this.exifToolFound.setValue(true);
		}
	}

	/**
	 * List of custom tags to be used when reading metadata
	 */
	public enum CustomTags implements Tag
	{
		PITCH("Pitch", Type.DOUBLE),
		ROLL("Roll", Type.DOUBLE),
		YAW("Yaw", Type.DOUBLE),
		SPEED_X("SpeedX", Type.DOUBLE),
		SPEED_Y("SpeedY", Type.DOUBLE),
		SPEED_Z("SpeedZ", Type.DOUBLE),
		CAMERA_MODEL_NAME("Model", Type.STRING),
		ALL_METADATA("AllMetadata", Type.STRING);


		/**
		 * Used to get the name of the tag (e.g. "Orientation", "ISO", etc.).
		 */
		private final String name;

		/**
		 * Used to get a hint for the native type of this tag's value as
		 * specified by Phil Harvey's <a href="http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/index.html">ExifTool Tag Guide</a>.
		 */
		private final CustomTags.Type type;

		/**
		 * Constructor just initializes the metadata field name and field type
		 *
		 * @param name The field name
		 * @param type The field type
		 */
		CustomTags(String name, CustomTags.Type type)
		{
			this.name = name;
			this.type = type;
		}

		/**
		 * Getter for tag name
		 *
		 * @return Getter for tag name
		 */
		@Override
		public String getName()
		{
			return name;
		}

		/*
			Implements the "getDisplayName" function in Tag.
			Added after upgrading to ExifTool Java Integration 2.5.0
		 */
		public String getDisplayName()
		{
			return name;
		}

		/**
		 * Function used to parse a string value into a type T
		 *
		 * @param value Converts a basic tag value into a type T
		 * @param <T> The type to convert into
		 * @return The converted type
		 */
		@Override
		public <T> T parse(String value)
		{
			return type.parse(value);
		}

		/**
		 * Utility type used to represent EXIF tag types
		 */
		@SuppressWarnings("unchecked")
		private enum Type
		{
			// Integer type just parses string into an integer
			INTEGER
			{
				@Override
				public <T> T parse(String value)
				{
					return (T) Integer.valueOf(Integer.parseInt(value));
				}
			},
			// Double type just parses string into a double
			DOUBLE
			{
				@Override
				public <T> T parse(String value)
				{
					return (T) Double.valueOf(Double.parseDouble(value));
				}
			},
			// String type just parses string into a string
			STRING
			{
				@Override
				public <T> T parse(String value) {
					return (T) value;
				}
			};

			/**
			 * Function to be implemented by our enum
			 *
			 * @param value Converts a basic tag value into a type T
			 * @param <T> The type to convert into
			 * @return The converted type
			 */
			public abstract <T> T parse(String value);
		}
	}

	/**
	 * Function used to read a file's metadata
	 *
	 * @param imageFile The file to read
	 * @return The image's metadata as a map
	 * @throws IOException If the image cannot be read, throw an exception
	 */
	public Map<Tag, String> readImageMetadata(File imageFile) throws IOException
	{
		// This is a list of our standard tags
		List<Tag> tags = new ArrayList<>(Arrays.asList(StandardTag.values()));
		// Add our custom tags
		tags.addAll(Arrays.asList(CustomTags.values()));
		// Ask exiftool to get our image's metadata
		// The map returned by this call is unmodifiable. Dunno why.
		Map<Tag, String> unmodifiableMap = this.exifTool.getImageMeta(imageFile, tags);

		// Add a special tag to retval which contains all metadata found in the image.
		Tag allMeta = CustomTags.ALL_METADATA;
		String allData = this.exifTool.getImageMeta(imageFile).toString();

		// Create a modifiable copy of what exifTool returned, and add a string containing all the metadata to it.
		// TODO: Better choice than Hashtable?
		Map<Tag, String> retval = new Hashtable<>();
		retval.putAll(unmodifiableMap);
		retval.put(allMeta, allData);

		return retval;
	}

	/**
	 * @return True if exiftool is found, or false otherwise
	 */
	public boolean isExifToolFound()
	{
		return exifToolFound.getValue();
	}

	/**
	 * @return Property representing if we have located the exiftool executable or no
	 */
	public ReadOnlyBooleanProperty exifToolFoundProperty()
	{
		return exifToolFound.getReadOnlyProperty();
	}

	/**
	 * Called to stop the ExifTool process
	 */
	public void shutdown()
	{
		if (this.exifTool != null)
		{
			// Close the exiftool process
			try
			{
				this.exifTool.close();
			}
			catch (Exception ignored) {}
		}
	}
}
