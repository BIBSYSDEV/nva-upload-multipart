package no.unit.nva.amazon.s3.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import nva.commons.utils.JacocoGenerated;

public final class S3Utils {

    private S3Utils() {
    }

    /**
     * Create a client to access Amazon S3 storage.
     * @return client to access S3 storage
     */
    @JacocoGenerated
    public static AmazonS3 createAmazonS3Client(String region) {
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build();
    }

}
