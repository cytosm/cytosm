package org.cytosm.common.gtop.io;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cytosm.common.gtop.GTop;

/***
 * Serializes a gtop.
 *
 *
 */
public final class SerializationInterface {

    /**
     * Default constructor.
     */
    private SerializationInterface() {}

    /**
     * Reads a GTop from a file.
     *
     * @param fileObj The file object
     * @return the corresponding GTop
     */
    public static GTop read(final File fileObj) {

        GTop gtop = null;

        ObjectMapper mapper = new ObjectMapper();

        try {
            gtop = mapper.readValue(fileObj, GTop.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return gtop;
    }

    /**
     * Reads a GTop from a string.
     *
     * @param gTopStr The strip to construct a GTop from
     * @return the corresponding GTop
     */
    public static GTop read(final String gTopStr) {

        GTop gtop = null;

        ObjectMapper mapper = new ObjectMapper();
        try {
            gtop = mapper.readValue(gTopStr, GTop.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gtop;
    }

    /**
     * pretty prints the Gtop to JSON String.
     *
     * @param gfile file to print
     * @return String respresentation of the gTop
     */
    public static String toPrettyString(final GTop gfile) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(gfile);
        } catch (Exception ex) {
            return gfile.toString();
        }
    }
}
