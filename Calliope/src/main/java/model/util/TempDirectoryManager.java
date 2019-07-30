package model.util;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Class used in managing temporary files created by Calliope
 */
public class TempDirectoryManager
{
	// The temporary folder to put all temporary Calliope files into
	private File calliopeTempDir;

	/**
	 * Constructor initializes the temporary directory
	 */
	public TempDirectoryManager(ErrorDisplay errorDisplay)
	{
		try
		{
			// We need to delete it after exiting!
			this.calliopeTempDir = Files.createTempDirectory("Calliope").toFile();
			this.calliopeTempDir.deleteOnExit();
		}
		catch (IOException e)
		{
			errorDisplay.notify("Error creating a temporary Calliope directory!\n" + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * The method that creates a new temp file
	 *
	 * @param fileName The name of the file to create
	 * @return A reference to the temporary file we created
	 */
	public File createTempFile(String fileName)
	{
		// The temporary file will have the temp directory as a parent and the same name except with 10 random alphanumeric characters tagged onto the end
		File tempFile = FileUtils.getFile(this.calliopeTempDir, FilenameUtils.getBaseName(fileName) + RandomStringUtils.randomAlphanumeric(10) + "." + FilenameUtils.getExtension(fileName));
		// If it exists, try again to ensure we get a unique file
		while (tempFile.exists())
			tempFile = FileUtils.getFile(this.calliopeTempDir, FilenameUtils.getBaseName(fileName) + RandomStringUtils.randomAlphanumeric(10) + "." + FilenameUtils.getExtension(fileName));

		// Delete the file when we exit
		tempFile.deleteOnExit();
		return tempFile;
	}

	/**
	 * Given a filename to create and a directory
	 *
	 * NOTE: Since these files will be nested away in a directory, we assume there will be no overlap in naming.
	 *   Thus, no random strings are added to their filenames.
	 *
	 * @param fileName The name of the file to create
	 *                 dsa
	 * @param mainDir
	 *                 dsa
	 * @return A reference to the temporary file we created
	 */
	public File createTempFileInDir(String fileName, String mainDir)
	{
		File fullDir = FileUtils.getFile(this.calliopeTempDir, mainDir);
		System.err.printf("Testing fullDir[%s]\n", fullDir);
		if(!fullDir.isDirectory() && !fullDir.mkdir()) {
			File mainFile = FileUtils.getFile(mainDir);
			createTempFileInDir("", mainFile.getParent());
		}

		// TODO: Comment
		File tempFile = FileUtils.getFile(fullDir, FilenameUtils.getName(fileName));

		// Delete the file when we exit
		tempFile.deleteOnExit();
		return tempFile;
	}

	/**
	 * Attempts to force delete the temporary directory
	 */
	public void shutdown()
	{
		if (this.calliopeTempDir.exists())
		{
			try
			{
				FileDeleteStrategy.FORCE.delete(this.calliopeTempDir);
			}
			catch (IOException ignored) {}
		}
	}
}
