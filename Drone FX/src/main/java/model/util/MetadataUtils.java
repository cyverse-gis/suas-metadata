package model.util;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Class containing utils for writing & reading metadata
 */
public class MetadataUtils
{
	private static ExifTool exifToolInstance = new ExifToolBuilder().enableStayOpen().withPath("C:\\Users\\David\\Desktop\\Software\\Sanimal\\Sanimal FX\\src\\main\\resources\\files\\exiftool.exe").build();

	public static Map<Tag, String> readImageMetadata(File imageFile) throws IOException
	{
		return exifToolInstance.getImageMeta(imageFile, Arrays.asList(StandardTag.values()));
	}
}
