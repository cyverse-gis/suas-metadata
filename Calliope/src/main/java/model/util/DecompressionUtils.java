package model.util;


import model.CalliopeData;
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
 * TODO: Comment
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

        // Create the ImageDirectory that will be returned, which will contain all the files as children
        ImageDirectory retval = new ImageDirectory(comTempDir);

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
                    if (!tempFile.isDirectory() && !tempFile.mkdir())
                        System.err.printf("TODO: Error message 1\n");
                    else {
                        ImageDirectory id = new ImageDirectory(tempFile);
                        retval.addChild(id);
                    }
                }
                else
                {
                    File tempParent = tempFile.getParentFile();
                    if (!tempParent.isDirectory() && !tempParent.mkdir())
                        System.err.printf("TODO: Error message 2\n");

                    OutputStream out = Files.newOutputStream(tempFile.toPath());
                    IOUtils.copy(ais, out);

                    ImageEntry ie = new ImageEntry(tempFile);
                    retval.addChild(ie);
                }
            }
        }
        catch(FileNotFoundException fnfe) { fnfe.printStackTrace(); }
        catch(IOException ioe) { ioe.printStackTrace(); }

        return retval;
    }


    // TODO: COMMENT
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

        // TODO: COMMENT
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
     * Test if a file is compressed by extracting its extension and testing that.
     *
     * @param file
     *            The file to test
     * @return True if the file is compressed, false if not
     */
    public static boolean fileIsCompressed(File file) {
        String toTest = FilenameUtils.getExtension(file.getName());
        return StringUtils.endsWithAny(toTest, CompressionTypes.getSupportedExtensions());
    }

    // TODO: COMMENT
    public static CompressionTypes getType(String input)
    {
        for (CompressionTypes currType : CompressionTypes.values()) {
            if (input.endsWith(currType.getExt()))
                return currType;
        }
        return null;
    }
}
