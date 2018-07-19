package model.constant;

import javafx.scene.input.DataFormat;

public class CalliopeDataFormats
{
	// Data formats are used for drag and drop

	// Store the site code
	public static final DataFormat SITE_CODE_FORMAT = new DataFormat("com.dslovikosky.site.siteCode");
	// Store the ImageDirectories file
	public static final DataFormat IMAGE_DIRECTORY_FILE_FORMAT = new DataFormat("com.dslovikosky.image.imageDirectoryFile");
	// Store the polygon point latitude
	public static final DataFormat POLYGON_LATITUDE_FORMAT = new DataFormat("com.dslovikosky.location.latitude");
	// Store the polygon point longitude
	public static final DataFormat POLYGON_LONGITUDE_FORMAT = new DataFormat("com.dslovikosky.location.longitude");
	// Store the 'awaiting polygon' flag
	public static final DataFormat AWAITING_POLYGON = new DataFormat("com.dslovikosky.location.awaiting");
}
