package controller;

import model.DroneLogger;
import model.MetadataIndexer;
import model.MetadataParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Primary class entry point into the program
 */
public class DroneMetadata
{
	private static final String[] ACCEPTED_EXTENSIONS = { "jpg", "JPG", "jpeg", "JPEG", "tiff", "TIFF", "tif", "TIF", "psd", "PSD", "png", "PNG", "bmp", "BMP", "gif", "GIF", "ico", "ICO" };

	/**
	 * Main expects one command line argument, a file or directory to index
	 *
	 * @param args Should contain a single argument with a file name
	 */
	public static void main(String[] args)
	{
		// Expect one command line argument with the name of the file to process
		if (args.length == 0)
		{
			DroneLogger.logError("Too few command line arguments given, execute with one argument, the file to indexSingle!");
			System.exit(1);
		}

		// We got too many arguments
		if (args.length > 1)
		{
			DroneLogger.logError("Too many command line arguments given, execute with one argument, the file to indexSingle!");
			System.exit(1);
		}

		// Pull the one command line argument
		String filePath = args[0];
		// Create the file
		File fileToIndex = new File(filePath);

		// Make sure the file exists
		if (!fileToIndex.exists())
		{
			DroneLogger.logError("Given file does not exist! (" + filePath + ")");
			System.exit(1);
		}

		// Make sure we can read the file
		if (!fileToIndex.canRead())
		{
			DroneLogger.logError("Given file is unreadable! (" + filePath + ")");
			System.exit(1);
		}

		// Process the file
		DroneLogger.logDebug("Input file/directory validated, beginning processing...");

		// If it's a directory, use bulk insert
		if (fileToIndex.isDirectory())
		{
			DroneMetadata.indexDirectory(fileToIndex);
		}
		// If it's a file use single insert
		else if (fileToIndex.isFile())
		{
			DroneMetadata.indexFile(fileToIndex);
		}
		// Do nothing
		else
		{
			DroneLogger.logError("File isn't a file or directory, what is it then?");
			System.exit(1);
		}
	}

	/**
	 * Given a valid file this function indexes it
	 *
	 * @param file The file to index into elastic search
	 */
	private static void indexFile(File file)
	{
		// Grab the file's extension, if it's valid we continue
		String extension = FilenameUtils.getExtension(file.getAbsolutePath());
		if (Arrays.stream(ACCEPTED_EXTENSIONS).anyMatch(extension::equals))
		{
			// The metadata parser used to parse the image file
			MetadataParser parser = new MetadataParser();
			// The raw metadata as key->value pairs from the parser
			Map<String, String> rawMetadata = parser.parse(file);
			// Print out a status message
			DroneLogger.logDebug("Metadata parsed, begin indexing...");
			// Index the metadata into elasticsearch
			MetadataIndexer indexer = new MetadataIndexer();
			indexer.indexSingle(rawMetadata);
		}
		// Invalid file extension so throw this file away
		else
		{
			DroneLogger.logError("File extension '" + extension + "' is not supported at this time!");
			System.exit(1);
		}
	}

	/**
	 * Works like indexFile, except it performs the operation on an entire directory recursively using bulk optimizations
	 *
	 * @param directory The directory to recursively index
	 */
	private static void indexDirectory(File directory)
	{
		// The files in a all subdirecties (recursive)
		Collection<File> files = FileUtils.listFiles(directory, ACCEPTED_EXTENSIONS, true);
		// Map the list of files to their metadata using the parser
		MetadataParser parser = new MetadataParser();
		List<Map<String, String>> rawMetadata = files.stream().map(parser::parse).collect(Collectors.toList());
		// Index these files in bulk
		MetadataIndexer indexer = new MetadataIndexer();
		indexer.indexBulk(rawMetadata);
	}
}