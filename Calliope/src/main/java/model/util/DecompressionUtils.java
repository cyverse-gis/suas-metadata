package model.util;


import model.CalliopeData;
import model.dataSources.DirectoryManager;
import model.image.ImageContainer;
import model.image.ImageDirectory;
import model.image.ImageEntry;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;

/**
 *
 *
 *
 * @author Jackson Lindsay
 */
public class DecompressionUtils
{
    /**
     * TODO: Comment
     *
     * @param file
     * @return
     *
     * And I just want to record that this (at the time incomplete) function worked exactly as I thought it would the first time I ran it.
     * What a miracle.
     */
    public static ImageDirectory decompressFile(File file)
    {
        // Get the temp directory manager, which will allow these files to be deleted on close
        TempDirectoryManager tdm = CalliopeData.getInstance().getTempDirectoryManager();

        // Take the compressed file's name and make it into a directory within the temp directory
        File comTempDir = tdm.createTempFileInDir(file.getName(), "");
        if(!comTempDir.mkdir())
        {
            System.err.printf("Failure in creating folder for compressed file [%s] within temp folder!\n", file.getName());
            return null;
        }

        try
        {
            // For the compressed file: Determine its file type, make an input stream for it,
            //   and generate an Archive Input Stream based on the file's compression type.
            CompressionTypes comType = getType(file.getName());
            FileInputStream fis = new FileInputStream(file);
            ArchiveInputStream ais = comType.getAIS(fis);

            // Iterate through the compressed file and extract all entries, adding them as children to "retval"
            // Reference used: https://commons.apache.org/proper/commons-compress/examples.html
            ArchiveEntry entry = null;
            while((entry = ais.getNextEntry()) != null)
            {
                String targetFile = FilenameUtils.getName(entry.getName());
                String targetPath = FilenameUtils.concat(comTempDir.getName(), FilenameUtils.getPath(entry.getName()));
                File tempFile = tdm.createTempFileInDir(targetFile, targetPath);
                System.err.printf("[%s] vs [%s]\n", entry.getName(), tempFile.getPath());

                if(entry.isDirectory())
                {
                    if (!tempFile.isDirectory())
                        System.err.printf("TODO: Error message 1\n");
                }
                else
                {
                    File tempParent = tempFile.getParentFile();
                    if (!tempParent.isDirectory())
                        System.err.printf("TODO: Error message 2\n");

                    OutputStream out = Files.newOutputStream(tempFile.toPath());
                    IOUtils.copy(ais, out);
                }
            }
        }
        catch(FileNotFoundException fnfe) { fnfe.printStackTrace(); }
        catch(IOException ioe) { ioe.printStackTrace(); }

        // Now that we've recreated the directory structure in the temp folder, import it via DirectoryManager
        ImageDirectory retval = DirectoryManager.loadDirectory(comTempDir);

        return retval;
    }


    /**
     * An enum for denoting the different types of compressed files that Calliope can extract.
     */
    public enum CompressionTypes
    {
        ZIP("zip"),
        TAR("tar");

        // TODO: Refactor into a sort of "strategy" design pattern, so the decompression class used is a variable rather than
        // the product of an if-statement
        public ArchiveInputStream getAIS(InputStream is)
        {
            if(this == CompressionTypes.ZIP)
                return new ZipArchiveInputStream(is);
            else if(this == CompressionTypes.TAR)
                return new TarArchiveInputStream(is);
            else
                return null;
        }

        private String ext;
        CompressionTypes(String e) { this.ext = e;}

        public String getExt() { return ext; }

        /**
         * @return An array containing all compressed extensions that are supported by Calliope
         */
        protected static String[] getSupportedExtensions()
        {
            String[] retval = new String[CompressionTypes.values().length * 2];

            int iter = 0;
            for (CompressionTypes currType : CompressionTypes.values()) {
                retval[iter] = currType.getExt();
                retval[iter+1] = currType.getExt().toUpperCase();
                iter += 2;
            }

            return retval;
        }
    }

    /**
     * Test if a file is compressed by testing its extension
     *
     * @param file
     *            The file to test
     * @return True if the file is compressed, false if not
     */
    public static boolean fileIsCompressed(File file) {
        String toTest = FilenameUtils.getExtension(file.getName());
        return StringUtils.endsWithAny(toTest, CompressionTypes.getSupportedExtensions());
    }

    /**
     * Given a filename as input, determines if the file is compressed in a way Calliope can work with
     *
     * @param input The filename to test
     * @return The corresponding CompressionType of the file, or null if file matches no type.
     */
    public static CompressionTypes getType(String input)
    {
        for (CompressionTypes currType : CompressionTypes.values()) {
            if (FilenameUtils.getExtension(input).compareTo(currType.getExt()) == 0)
                return currType;
        }
        return null;
    }
}
