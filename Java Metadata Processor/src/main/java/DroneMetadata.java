import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

public class DroneMetadata
{
	public static void main(String[] args)
	{
		// Expect one command line argument with the name of the file to process
		if (args.length == 0)
		{
			System.err.println("Too few command line arguments given, execute with one argument, the file to index!");
			System.exit(1);
		}

		if (args.length > 1)
		{
			System.err.println("Too many command line arguments given, execute with one argument, the file to index!");
			System.exit(1);
		}

		// Pull the one command line argument
		String filePath = args[0];
		File fileToIndex = new File(filePath);

		if (!fileToIndex.exists())
		{
			System.err.println("Given file does not exist! (" + filePath + ")");
			System.exit(1);
		}

		if (!fileToIndex.canRead())
		{
			System.err.println("Given file is unreadable! (" + filePath + ")");
			System.exit(1);
		}

		if (fileToIndex.isDirectory())
		{
			System.err.println("This script must be called on a file, not a directory!");
			System.exit(1);
		}

		System.out.println("File validated, beginning processing...");

		MetadataParser parser = new MetadataParser();
		Map<String, String> rawMetadata = parser.parse(fileToIndex);

		System.out.println("Metadata parsed, begin indexing...");

		MetadataIndexer indexer = new MetadataIndexer();
		indexer.index(rawMetadata);
	}
}