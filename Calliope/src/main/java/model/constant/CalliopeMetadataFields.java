package model.constant;

import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoShort;

import java.time.format.DateTimeFormatter;

public class CalliopeMetadataFields
{
	// The metadata field representing a flag that tells us that the image has been tagged with Calliope data
	public static final TagInfoShort CALLIOPE = new TagInfoShort("Calliope Flag", 551, TiffDirectoryType.EXIF_DIRECTORY_UNKNOWN);
	// The metadata field representing what species entries are added to an image
	public static final TagInfoAscii SPECIES_ENTRY = new TagInfoAscii("Species Entry", 552, -1, TiffDirectoryType.EXIF_DIRECTORY_UNKNOWN);
	// The metadata field representing the location that a species was tagged at
	public static final TagInfoAscii LOCATION_ENTRY = new TagInfoAscii("Location Name", 553, -1, TiffDirectoryType.EXIF_DIRECTORY_UNKNOWN);

	public static final DateTimeFormatter INDEX_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
}
