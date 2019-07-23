package model.dataSources.localPC.compressed;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.ArrayList;

/**
 * TODO: Comment
 *
 *
 * @author Jackson Lindsay
 */
public class DecompressionUtils
{
    public enum CompressionTypes
    {
        ZIP("zip"),
        TAR("tar");

        private final String ext;
        CompressionTypes(String e) { this.ext = e; }

        public String getExt() { return ext; }

        // TODO: COMMENT
        public static CompressionTypes getType(String input) {
            for (CompressionTypes currType : CompressionTypes.values()) {
                if (input.endsWith(currType.getExt()))
                    return currType;
            }
            return null;
        }

        // TODO: COMMENT
        public static String[] getAllExtensions()
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
        return StringUtils.endsWithAny(toTest, );
    }
}
