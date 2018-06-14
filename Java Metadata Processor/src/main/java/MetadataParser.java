import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.properties.XMPPropertyInfo;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.xmp.XmpDirectory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MetadataParser
{
	public Map<String, String> parse(File fileToIndex)
	{
		Map<String, String> metadataMap = new HashMap<>();
		try
		{
			Metadata metadata = JpegMetadataReader.readMetadata(fileToIndex);

			for (Directory directory : metadata.getDirectories())
			{
				for (Tag tag : directory.getTags())
				{
					String description = tag.getDescription();
					if (description == null)
						description = directory.getString(tag.getTagType()) + " (unable to formulate description)";
					metadataMap.put("[" + tag.getDirectoryName() + "] " + tag.getTagName(), description);
				}

				if (directory instanceof XmpDirectory)
				{
					XmpDirectory xmpDirectory = (XmpDirectory) directory;
					XMPMeta xmpMeta = xmpDirectory.getXMPMeta();
					try
					{
						XMPIterator xmpIterator = xmpMeta.iterator();
						while (xmpIterator.hasNext())
						{
							XMPPropertyInfo propertyInfo = (XMPPropertyInfo) xmpIterator.next();
							metadataMap.put("[XMP Property] " + propertyInfo.getPath(), propertyInfo.getValue());
						}
					}
					catch (XMPException e)
					{
						System.err.println("Could not read the XMP metadata!");
						e.printStackTrace();
					}
				}

				for (String error : directory.getErrors())
				{
					System.err.println("Directory Error: " + error);
				}
			}
		}
		catch (ImageProcessingException | IOException e)
		{
			System.err.println("Could not process the image metadata!");
			e.printStackTrace();
			System.exit(1);
		}
		return metadataMap;
	}
}
