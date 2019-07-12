package model.dataSources;

import javafx.beans.property.DoubleProperty;
import model.CalliopeData;
import model.image.DataContainer;
import model.image.DataDirectory;
import model.image.ImageEntry;
import model.image.VideoEntry;
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
	public static void removeEmptyDirectories(DataContainer directory)
	{
		// Go through each child
		for (int i = 0; i < directory.getChildren().size(); i++)
		{
			// Grab the current image container
			DataContainer dataContainer = directory.getChildren().get(i);
			// If the container is a directory, check if it's empty
			if (dataContainer instanceof DataDirectory)
			{
				// Remove empty directories from this directory
				DirectoryManager.removeEmptyDirectories(dataContainer);
				// If it's empty, remove this directory and reduce I since we don't want to get an index out of bounds exception
				if (dataContainer.getChildren().isEmpty())
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
	 * @param dataDirectory The directory containing images that need initializing
	 * @param progressProperty How many images we've parsed so far
	 */
	public static void initImages(DataDirectory dataDirectory, DoubleProperty progressProperty)
	{
		// List of images to init
		List<DataContainer> imageEntries = dataDirectory.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry || imageContainer instanceof VideoEntry).collect(Collectors.toList());
		// The total number of images in the list
		Integer imageCount = imageEntries.size();
		for (Integer i = 0; i < imageCount; i++)
		{
			// Every 20 images we update progress
			if (i % 20 == 0)
				progressProperty.setValue(i.doubleValue() / imageCount.doubleValue());
			// Read the metadata into each image
			if (imageEntries.get(i) instanceof ImageEntry) {
				((ImageEntry)imageEntries.get(i)).readFileMetadataFromImage();
			} else {
				((VideoEntry)imageEntries.get(i)).readFileMetadataFromVideo();
			}
		}
	}

	/**
	 * Reads a set of files and returns them in a usable format
	 *
	 * @param files
	 *            The files to make into a directory. We assume all files are in the same directory
	 */
	public static DataDirectory loadFiles(List<File> files)
	{
		// Map our input to a list of images only
		List<File> validImages = files.stream().filter(AnalysisUtils::fileIsImage).collect(Collectors.toList());
		List<File> validMedia = files.stream().filter(AnalysisUtils::fileIsMedia).collect(Collectors.toList());
		// Make sure we have at least one image file
		if (!(validImages.isEmpty() && validMedia.isEmpty()))
		{
			// All files are in the same directory, so grab the first file's parent directory
			DataDirectory dataDirectory = new DataDirectory(files.get(0).getParentFile());
			// Iterate over the images
			for (File validImage : validImages)
			{
				// Load the image file and metadata and add it to the directory
				ImageEntry imageEntry = new ImageEntry(validImage);
				dataDirectory.addChild(imageEntry);
			}
			// Iterate over the images
			for (File validVideo : validMedia)
			{
				// Load the image file and metadata and add it to the directory
				VideoEntry videoEntry = new VideoEntry(validVideo);
				dataDirectory.addChild(videoEntry);
			}
			// Return the directory
			return dataDirectory;
		}
		return null;
	}

	/**
	 * Reads a directory recursively and returns it in a usable format
	 * 
	 * @param imageOrLocation
	 *            The file to make into a directory
	 */
	public static DataDirectory loadDirectory(File imageOrLocation)
	{
		// If it is not a valid file or directory, returns null
		DataDirectory toReturn = null;
		if (imageOrLocation.isDirectory())
		{
			// If it is a directory, recursively create it
			toReturn = new DataDirectory(imageOrLocation);
			DirectoryManager.createDirectoryAndImageTree(toReturn);
		}
		else if (AnalysisUtils.fileIsImage(imageOrLocation)) {
			// If it's not a directory, test if it's an image
			toReturn = new DataDirectory(imageOrLocation.getParentFile());
			ImageEntry imageEntry = new ImageEntry(imageOrLocation);
			toReturn.addChild(imageEntry);
		}
		else if (AnalysisUtils.fileIsMedia(imageOrLocation)) {
			// If it's not a directory or an image, test if it's a video
			toReturn = new DataDirectory(imageOrLocation.getParentFile());
			VideoEntry videoEntry = new VideoEntry(imageOrLocation);
			toReturn.addChild(videoEntry);
		}
		return toReturn;
	}

	/**
	 * Recursively create the directory structure
	 * 
	 * @param current
	 *            The current directory to work on
	 */
	private static void createDirectoryAndImageTree(DataDirectory current)
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
				// Add all video files to the directory
				else if (AnalysisUtils.fileIsMedia(file))
				{
					VideoEntry videoEntry = new VideoEntry(file);
					current.addChild(videoEntry);
				}
				// Add all subdirectories to the directory
				else if (file.isDirectory())
				{
					DataDirectory subDirectory = new DataDirectory(file);
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
	public static File[] directoryToTars(DataDirectory directory, Integer maxImagesPerTar)
	{
		maxImagesPerTar = maxImagesPerTar - 1;
		try
		{
			// List of files to be uploaded
			List<DataContainer> entries = directory.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry || imageContainer instanceof VideoEntry).collect(Collectors.toList());

			// Take the number of images and videos / maximum number of files per tar to get the number of tar files we need
			Integer numberOfTars = (int) Math.ceil((double) entries.size() / (double) maxImagesPerTar);
			Integer imagesPerTar = (int) Math.ceil((double) entries.size() / (double) numberOfTars);
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

				for (Integer entryIndex = tarIndex * imagesPerTar; entryIndex < (tarIndex + 1) * imagesPerTar && entryIndex < entries.size(); entryIndex++)
				{
					DataContainer entry = entries.get(entryIndex);
					// Create an archive entry for the image
					String tarPath = StringUtils.substringAfter(entry.getFile().getAbsolutePath(), topDirectory).replace('\\', '/');
					ArchiveEntry archiveEntry = tarOut.createArchiveEntry(entry.getFile(), tarPath);
					// Put the archive entry into the TAR file
					tarOut.putArchiveEntry(archiveEntry);
					// Write all the bytes in the file into the TAR file
					tarOut.write(Files.readAllBytes(entry.getFile().toPath()));
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
