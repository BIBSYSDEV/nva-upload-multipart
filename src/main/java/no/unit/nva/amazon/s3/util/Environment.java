package no.unit.nva.amazon.s3.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.util.Optional;

public class Environment {

    public static final String DEFAULT_AWS_REGION = Regions.EU_WEST_1.getName();
    public static final String AWS_REGION_KEY = "AWS_REGION";
    public static final String ALLOWED_ORIGIN_KEY = "ALLOWED_ORIGIN";
    public static final String S3_UPLOAD_BUCKET_KEY = "S3_UPLOAD_BUCKET";
    public static final String MISSING_ENV_TEXT = "Missing environment variable %s";

    /**
     * Create a client to access Amazon S3 storage.
     * @return client to access S3 storage
     */
    public AmazonS3 createAmazonS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(get(AWS_REGION_KEY).get())
                .build();
    }

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
