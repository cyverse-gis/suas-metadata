import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.properties.XMPPropertyInfo;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.xmp.XmpDirectory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used to parse an image file's metadata
 */
public class MetadataParser
{
	/**
	 * Given a file to parse, this method returns a mapping of raw key->value metadata pairs found on the image
	 *
	 * @param fileToParse The file to read metadata from
	 * @return A mapping of exif key -> exif value pairs
	 */
	public Map<String, String> parse(File fileToParse)
	{
		// Create a map of metadata objects
		Map<String, String> metadataMap = new HashMap<>();
		try
		{
			// Read the file's metadata
			Metadata metadata = ImageMetadataReader.readMetadata(fileToParse);

			// Iterate over metadata directories
			for (Directory directory : metadata.getDirectories())
			{
				// For each metadata tag add '[Directory] TagName' -> 'TagValue' as a metadata entry
				for (Tag tag : directory.getTags())
				{
					// This is taken from tag.toString
					String description = tag.getDescription();
					if (description == null)
						description = directory.getString(tag.getTagType()) + " (unable to formulate description)";
					// Put  '[Directory] TagName' -> 'TagValue'
					metadataMap.put("[" + tag.getDirectoryName() + "] " + tag.getTagName(), description);
				}

				// If the directory is XMP, it's unstructured and must be parsed separately
				if (directory instanceof XmpDirectory)
				{
					// Grab the XMP directory
					XmpDirectory xmpDirectory = (XmpDirectory) directory;
					// Grab unstructured XMP metadata
					XMPMeta xmpMeta = xmpDirectory.getXMPMeta();
					try
					{
						// Grab the iterator that goes over the XMP metadata
						XMPIterator xmpIterator = xmpMeta.iterator();
						while (xmpIterator.hasNext())
						{
							// Grab the XMP properties info
							XMPPropertyInfo propertyInfo = (XMPPropertyInfo) xmpIterator.next();
							// Add it to our mapping
							metadataMap.put("[XMP Property] " + propertyInfo.getPath(), propertyInfo.getValue());
						}
					}
					// There was an exception, print it but keep going
					catch (XMPException e)
					{
						DroneLogger.logError("Could not read the XMP metadata!");
						e.printStackTrace();
					}
				}

				// If our directory had any errors, print those
				for (String error : directory.getErrors())
				{
					DroneLogger.logError("Directory Error: " + error);
				}
			}
		}
		// If the image could not be processed skip it and print an error
		catch (ImageProcessingException | IOException e)
		{
			DroneLogger.logError("Could not process the image metadata! File is " + fileToParse.getAbsolutePath());
		}
		return metadataMap;
	}
}
