package no.unit.nva.amazon.s3;

import java.util.Optional;

public class Environment {

    public static final String ALLOWED_ORIGIN_KEY = "ALLOWED_ORIGIN";
    public static final String S3_UPLOAD_BUCKET_KEY = "S3_UPLOAD_BUCKET";
    public static final String MISSING_ENV_TEXT = "Missing environment variable %s";

    /**
     * Get environment variable.
     *
     * @param name  name of environment variable
     * @return  optional with value of environment variable
     */
    public Optional<String> get(String name) {
        String environmentVariable = System.getenv(name);

        return  Optional.ofNullable(environmentVariable);
    }
}
