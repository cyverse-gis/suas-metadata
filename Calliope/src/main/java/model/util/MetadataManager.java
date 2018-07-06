package model.util;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import controller.Calliope;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Class containing utils for writing & reading metadata
 */
public class MetadataManager
{
	// A reference to the EXIF tool object used to read and write metadata
	private final ExifTool exifTool;

	/**
	 * Metadata Manager constructor just starts the exif tool process
	 */
	public MetadataManager()
	{
		this.exifTool = new ExifToolBuilder().enableStayOpen().withPath(Calliope.class.getClass().getResource("/files/exiftool.exe").getFile()).build();
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
		CAMERA_MODEL_NAME("Model", Type.STRING);

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
		return this.exifTool.getImageMeta(imageFile, tags);
	}

	/**
	 * Finalize is called like a deconstructor which closes the exiftool process
	 *
	 * @throws Throwable Throws an exception if something goes wrong
	 */
	@Override
	protected void finalize() throws Throwable
	{
		super.finalize();
		// Close the exiftool process
		this.exifTool.close();
	}
}
