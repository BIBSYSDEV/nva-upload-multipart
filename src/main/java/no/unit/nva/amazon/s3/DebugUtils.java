package no.unit.nva.amazon.s3;

import java.io.PrintWriter;
import java.io.StringWriter;

public class DebugUtils {

    /**
     * Writes a stackTrace into a string.
     * @param e any Exception
     * @return Stringdump of exception
     */
    public static String dumpException(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

}
