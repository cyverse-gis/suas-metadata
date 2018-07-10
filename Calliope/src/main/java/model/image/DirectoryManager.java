package model.image;

import model.CalliopeData;
import model.analysis.CalliopeAnalysisUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A class that imports images into a more easily readable structure
 * 
 * @author David Slovikosky
 */
public class DirectoryManager
{
	/**
	 * Given a directory this function validates that each file exists and if they don't adds them to the invalid containers list
	 *
	 * @param directory The directory to validate
	 * @param invalidContainers The invalid containers in this directory
	 */
	public static void performDirectoryValidation(ImageContainer directory, List<ImageContainer> invalidContainers)
	{
		if (invalidContainers == null)
			return;

		// Ensure that the file exists, otherwise add it to the invalid containers list
		if (!directory.getFile().exists())
			invalidContainers.add(directory);

		// Go through each of the children and validate them
		for (ImageContainer container : directory.getChildren())
			DirectoryManager.performDirectoryValidation(container, invalidContainers);
	}

	/**
	 * Wipe out any empty sub-directories
	 *
	 * @param directory The directory to remove empty sub-directories from
	 */
	public static void removeEmptyDirectories(ImageDirectory directory)
	{
		// Go through each child
		for (int i = 0; i < directory.getChildren().size(); i++)
		{
			// Grab the current image container
			ImageContainer imageContainer = directory.getChildren().get(i);
			// If it's a directory, recursively remove image directories from it
			if (imageContainer instanceof ImageDirectory)
			{
				// Remove empty directories from this directory
				DirectoryManager.removeEmptyDirectories((ImageDirectory) imageContainer);
				// If it's empty, remove this directory and reduce I since we don't want to get an index out of bounds exception
				if (imageContainer.getChildren().isEmpty())
				{
					directory.getChildren().remove(i);
					i--;
				}
			}
		}
	}

	/**
	 * Set the head directory to the given file
	 * 
	 * @param imageOrLocation
	 *            The file to make into a directory
	 */
	public static ImageDirectory loadDirectory(File imageOrLocation)
	{
		ImageDirectory toReturn;
		if (!imageOrLocation.isDirectory())
		{
			// If it's not a directory, then just add the image
			toReturn = new ImageDirectory(imageOrLocation.getParentFile());
			ImageEntry imageEntry = new ImageEntry(imageOrLocation);
			imageEntry.readFileMetadataIntoImage();
			imageEntry.initIconBindings();
			toReturn.addImage(imageEntry);
		}
		else
		{
			// If it is a directory, recursively create it
			toReturn = new ImageDirectory(imageOrLocation);
			DirectoryManager.createDirectoryAndImageTree(toReturn);
		}
		return toReturn;
	}

	/**
	 * Recursively create the directory structure
	 * 
	 * @param current
	 *            The current directory to work on
	 */
	private static void createDirectoryAndImageTree(ImageDirectory current)
	{
		File[] subFiles = current.getFile().listFiles();

		if (subFiles != null)
		{
			// Get all files in the directory
			for (File file : subFiles)
			{
				// Add all image files to the directory
				if (CalliopeAnalysisUtils.fileIsImage(file))
				{
					ImageEntry imageEntry = new ImageEntry(file);
					imageEntry.readFileMetadataIntoImage();
					imageEntry.initIconBindings();
					current.addImage(imageEntry);
				}
				// Add all subdirectories to the directory
				else if (file.isDirectory())
				{
					ImageDirectory subDirectory = new ImageDirectory(file);
					current.addChild(subDirectory);
					DirectoryManager.createDirectoryAndImageTree(subDirectory);
				}
			}
		}
	}

	/**
	 * Given an image directory, this will create a TAR file out of the directory
	 *
	 * @param directory The image directory to TAR
	 * @param imageToMetadata The CSV file representing each image's metadata
	 * @return The TAR file
	 */
	public static File[] directoryToTars(ImageDirectory directory, Function<ImageEntry, String> imageToMetadata, Integer maxImagesPerTar)
	{
		maxImagesPerTar = maxImagesPerTar - 1;
		try
		{
			// List of images to be uploaded
			List<ImageEntry> imageEntries = directory.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry).map(imageContainer -> (ImageEntry) imageContainer).collect(Collectors.toList());

			// Take the number of images / maximum number of images per tar to get the number of tar files we need
			Integer numberOfTars = (int) Math.ceil((double) imageEntries.size() / (double) maxImagesPerTar);
			Integer imagesPerTar = (int) Math.ceil((double) imageEntries.size() / (double) numberOfTars);
			// Create an array of tars
			File[] tars = new File[numberOfTars];

			// Get the path to the top level directory
			String topDirectory = directory.getFile().getParentFile().getAbsolutePath();

			for (Integer tarIndex = 0; tarIndex < numberOfTars; tarIndex++)
			{
				// Create a temporarily TAR file to write to
				File tempTar = CalliopeData.getInstance().getTempDirectoryManager().createTempFile("tarToUpload.tar");
				// Create a TAR output stream to write to
				TarArchiveOutputStream tarOut = new TarArchiveOutputStream(new FileOutputStream(tempTar));

				for (Integer imageIndex = tarIndex * imagesPerTar; imageIndex < (tarIndex + 1) * imagesPerTar && imageIndex < imageEntries.size(); imageIndex++)
				{
					ImageEntry imageEntry = imageEntries.get(imageIndex);
					// Create an archive entry for the image
					String tarPath = StringUtils.substringAfter(imageEntry.getFile().getAbsolutePath(), topDirectory).replace('\\', '/');
					ArchiveEntry archiveEntry = tarOut.createArchiveEntry(imageEntry.getFile(), tarPath);
					// Put the archive entry into the TAR file
					tarOut.putArchiveEntry(archiveEntry);
					// Write all the bytes in the file into the TAR file
					tarOut.write(Files.readAllBytes(imageEntry.getFile().toPath()));
					// Finish writing the TAR entry
					tarOut.closeArchiveEntry();
				}

				// Flush the file and close it. We delete the TAR after the program closes
				tarOut.flush();
				tarOut.close();

				// Store the tar path
				tars[tarIndex] = tempTar;
			}

			return tars;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		// If something goes wrong, return a blank array
		return new File[0];
	}
}
