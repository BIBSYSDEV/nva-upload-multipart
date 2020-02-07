package no.unit.nva.amazon.s3;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class DebugUtils {

    /**
     * Writes a stackTrace into a string.
     * @param e any Exception
     * @return Stringdump of exception
     */
    public static String dumpException(Exception e) {
        return  ExceptionUtils.getStackTrace(e);
    }

}
