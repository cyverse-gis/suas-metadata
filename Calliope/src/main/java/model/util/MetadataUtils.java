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
public class MetadataUtils
{
	private static final ExifTool EXIF_TOOL_INSTANCE;

	static
	{
		EXIF_TOOL_INSTANCE = new ExifToolBuilder().enableStayOpen().withPath(Calliope.class.getClass().getResource("/files/exiftool.exe").getFile()).build();
	}

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

		CustomTags(String name, CustomTags.Type type) {
			this.name = name;
			this.type = type;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public <T> T parse(String value) {
			return type.parse(value);
		}

		@SuppressWarnings("unchecked")
		private static enum Type {
			INTEGER {
				@Override
				public <T> T parse(String value) {
					return (T) Integer.valueOf(Integer.parseInt(value));
				}
			},
			DOUBLE {
				@Override
				public <T> T parse(String value) {
					return (T) Double.valueOf(Double.parseDouble(value));
				}
			},
			STRING {
				@Override
				public <T> T parse(String value) {
					return (T) value;
				}
			};

			public abstract <T> T parse(String value);
		}
	}

	public static Map<Tag, String> readImageMetadata(File imageFile) throws IOException
	{
		List<Tag> standardTags = new ArrayList<>(Arrays.asList(StandardTag.values()));
		standardTags.addAll(Arrays.asList(CustomTags.values()));
		return EXIF_TOOL_INSTANCE.getImageMeta(imageFile, standardTags);
	}
}
