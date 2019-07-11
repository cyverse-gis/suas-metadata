package model.dataSources;

import javafx.beans.property.DoubleProperty;
import model.CalliopeData;
import model.image.ImageContainer;
import model.image.ImageDirectory;
import model.image.ImageEntry;
import model.util.AnalysisUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class that imports images into a more easily readable structure
 * 
 * @author David Slovikosky
 */
public class DirectoryManager
{
	/**
	 * Wipe out any empty sub-directories
	 *
	 * @param directory The directory to remove empty sub-directories from
	 */
	public static void removeEmptyDirectories(ImageContainer directory)
	{
		// Go through each child
		for (int i = 0; i < directory.getChildren().size(); i++)
		{
			// Grab the current image container
			ImageContainer imageContainer = directory.getChildren().get(i);
			// If the container is a directory, check if it's empty
			if (imageContainer instanceof ImageDirectory)
			{
				// Remove empty directories from this directory
				DirectoryManager.removeEmptyDirectories(imageContainer);
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
	 * Initializes all images in a directory, we do this so we can get a progress bar
	 *
	 * @param imageDirectory The directory containing images that need initializing
	 * @param progressProperty How many images we've parsed so far
	 */
	public static void initImages(ImageDirectory imageDirectory, DoubleProperty progressProperty)
	{
		// List of images to init
		List<ImageEntry> imageEntries = imageDirectory.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry).map(imageContainer -> (ImageEntry) imageContainer).collect(Collectors.toList());
		// The total number of images in the list
		Integer imageCount = imageEntries.size();
		for (Integer i = 0; i < imageCount; i++)
		{
			// Every 20 images we update progress
			if (i % 20 == 0)
				progressProperty.setValue(i.doubleValue() / imageCount.doubleValue());
			// Read the metadata into each image
			imageEntries.get(i).readFileMetadataFromImage();
		}
	}

	/**
	 * Reads a set of files and returns them in a usable format
	 *
	 * @param files
	 *            The files to make into a directory. We assume all files are in the same directory
	 */
	public static ImageDirectory loadFiles(List<File> files)
	{
		// Map our input to a list of images only
		List<File> validImages = files.stream().filter(AnalysisUtils::fileIsImage).collect(Collectors.toList());
		// Make sure we have at least one image file
		if (!validImages.isEmpty())
		{
			// All files are in the same directory, so grab the first file's parent directory
			ImageDirectory imageDirectory = new ImageDirectory(files.get(0).getParentFile());
			// Iterate over the images
			for (File validImage : validImages)
			{
				// Load the image file and metadata and add it to the directory
				ImageEntry imageEntry = new ImageEntry(validImage);
				imageDirectory.addChild(imageEntry);
			}
			// Return the directory
			return imageDirectory;
		}
		return new ImageDirectory(null);
	}

	/**
	 * Reads a directory recursively and returns it in a usable format
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
			toReturn.addChild(imageEntry);
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
	 * Reads a set of files and returns them in a usable format
	 * Note that non-compressed files are culled out in this method
	 *
	 * @param files
	 *            The files to make into a directory. We assume all files are in the same directory
	 */
	public static ImageDirectory loadCompressed(List<File> files)
	{
		// Map our input to a list of images only
		List<File> validImages = files.stream().filter(AnalysisUtils::fileIsCompressed).collect(Collectors.toList());
		// Make sure we have at least one image file
		if (!validImages.isEmpty())
		{
			// All files are in the same directory, so grab the first file's parent directory
			ImageDirectory imageDirectory = new ImageDirectory(files.get(0).getParentFile());
			// Iterate over the images
			for (File validImage : validImages)
			{
				// Load the image file and metadata and add it to the directory
				ImageEntry imageEntry = new ImageEntry(validImage);
				imageDirectory.addChild(imageEntry);
			}
			// Return the directory
			return imageDirectory;
		}
		return new ImageDirectory(null);
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
				if (AnalysisUtils.fileIsImage(file))
				{
					ImageEntry imageEntry = new ImageEntry(file);
					current.addChild(imageEntry);
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
	 * @return The TAR file
	 */
	public static File[] directoryToTars(ImageDirectory directory, Integer maxImagesPerTar)
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
