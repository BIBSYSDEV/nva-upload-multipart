package no.unit.nva.amazon.s3;


import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import no.unit.nva.amazon.s3.exception.InvalidInputException;
import no.unit.nva.amazon.s3.exception.NotFoundException;
import no.unit.nva.amazon.s3.model.PrepareUploadPartRequestBody;
import no.unit.nva.amazon.s3.model.PrepareUploadPartResponseBody;
import no.unit.nva.amazon.s3.util.S3Constants;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import static java.util.Objects.requireNonNull;
import static no.unit.nva.amazon.s3.util.S3Constants.AWS_REGION_KEY;
import static no.unit.nva.amazon.s3.util.S3Utils.createAmazonS3Client;
import static org.apache.http.HttpStatus.SC_OK;


public class PrepareUploadPartHandler extends ApiGatewayHandler<PrepareUploadPartRequestBody,
        PrepareUploadPartResponseBody> {

    public static final String PARAMETER_UPLOAD_ID_KEY = "uploadId";
    public static final String PARAMETER_PART_NUMBER_KEY = "partNumber";

    private static final Logger logger = LoggerFactory.getLogger(PrepareUploadPartHandler.class);
    public static final String S3_ERROR = "S3 error";

    private final transient String bucketName;
    private final transient AmazonS3 s3Client;

    /**
     * Default constructor for PrepareUploadPartHandler.
     */
    @JacocoGenerated
    public PrepareUploadPartHandler() {
        this(new Environment());
    }

    /**
     * Constructor for PrepareUploadPartHandler.
     *
     * @param environment   environment reader
     */
    @JacocoGenerated
    public PrepareUploadPartHandler(Environment environment) {
        this(
                environment,
                createAmazonS3Client(environment.readEnv(AWS_REGION_KEY)),
                environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)
        );
    }

    /**
     * Construct for lambda eventhandler to create an upload request for S3.
     */
    public PrepareUploadPartHandler(Environment environment, AmazonS3 s3Client, String bucketName) {
        super(PrepareUploadPartRequestBody.class, environment, logger);
        this.bucketName = bucketName;
        this.s3Client = s3Client;
    }

    @Override
    protected PrepareUploadPartResponseBody processInput(PrepareUploadPartRequestBody input, RequestInfo requestInfo,
                                                         Context context) throws ApiGatewayException {
        validate(input);
        GeneratePresignedUrlRequest predesignedUrlUploadRequest = toGeneratePresignedUrlRequest(input);
        return new PrepareUploadPartResponseBody(getUrl(predesignedUrlUploadRequest));
    }

    private URL getUrl(GeneratePresignedUrlRequest predesignedUrlUploadRequest) throws NotFoundException {
        try {
            return s3Client.generatePresignedUrl(predesignedUrlUploadRequest);
        } catch (AmazonS3Exception e) {
            throw new NotFoundException(S3_ERROR, e);
        }
    }

    private void validate(PrepareUploadPartRequestBody input) throws ApiGatewayException {
        try {
            requireNonNull(input);
            requireNonNull(input.getKey());
            requireNonNull(input.getUploadId());
            requireNonNull(input.getNumber());
            requireNonNull(input.getBody());
        } catch (Exception e) {
            throw new InvalidInputException(e);
        }
    }

    private GeneratePresignedUrlRequest toGeneratePresignedUrlRequest(PrepareUploadPartRequestBody input) {
        GeneratePresignedUrlRequest predesignedUrlUploadRequest =
                new GeneratePresignedUrlRequest(
                        bucketName,
                        input.getKey()
                ).withMethod(HttpMethod.PUT);
        predesignedUrlUploadRequest.addRequestParameter(PARAMETER_UPLOAD_ID_KEY, input.getUploadId());
        predesignedUrlUploadRequest.addRequestParameter(PARAMETER_PART_NUMBER_KEY, input.getNumber());
        return predesignedUrlUploadRequest;
    }

    @Override
    protected Integer getSuccessStatusCode(PrepareUploadPartRequestBody input, PrepareUploadPartResponseBody output) {
        return SC_OK;
    }
}
