package no.unit.nva.amazon.s3;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import no.unit.nva.amazon.s3.exception.InvalidInputException;
import no.unit.nva.amazon.s3.exception.NotFoundException;
import no.unit.nva.amazon.s3.model.AbortMultipartUploadRequestBody;
import no.unit.nva.amazon.s3.model.SimpleMessageResponse;
import no.unit.nva.amazon.s3.util.S3Constants;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static no.unit.nva.amazon.s3.util.S3Constants.AWS_REGION_KEY;
import static no.unit.nva.amazon.s3.util.S3Utils.createAmazonS3Client;
import static org.apache.http.HttpStatus.SC_OK;

public class AbortMultipartUploadHandler extends ApiGatewayHandler<AbortMultipartUploadRequestBody,
        SimpleMessageResponse> {

    public static final String MULTIPART_UPLOAD_ABORTED_MESSAGE = "Multipart Upload aborted";

    private static final Logger logger = LoggerFactory.getLogger(AbortMultipartUploadHandler.class);
    public static final String S3_ERROR = "S3 error";

    public final String bucketName;
    private final AmazonS3 s3Client;

    /**
     * Default constructor for AbortMultipartUploadHandler.
     */
    @JacocoGenerated
    public AbortMultipartUploadHandler() {
        this(new Environment());
    }

    /**
     * Constructor for AbortMultipartUploadHandler.
     *
     * @param environment   environment reader
     */
    @JacocoGenerated
    public AbortMultipartUploadHandler(Environment environment) {
        this(
                environment,
                createAmazonS3Client(environment.readEnv(AWS_REGION_KEY)),
                environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)
        );
    }

    /**
     * Construct for lambda eventhandler to create an upload request for S3.
     */
    public AbortMultipartUploadHandler(Environment environment, AmazonS3 s3Client, String bucketName) {
        super(AbortMultipartUploadRequestBody.class, environment, logger);
        this.bucketName = bucketName;
        this.s3Client = s3Client;
    }

    @Override
    protected SimpleMessageResponse processInput(AbortMultipartUploadRequestBody input, RequestInfo requestInfo,
                                                 Context context) throws ApiGatewayException {
        validate(input);
        abortMultipartUpload(toAbortMultipartUploadRequest(input));
        return new SimpleMessageResponse(MULTIPART_UPLOAD_ABORTED_MESSAGE);
    }

    protected void abortMultipartUpload(AbortMultipartUploadRequest abortMultipartUploadRequest)
            throws NotFoundException {
        try {
            s3Client.abortMultipartUpload(abortMultipartUploadRequest);
        } catch (AmazonS3Exception e) {
            throw new NotFoundException(S3_ERROR, e);
        }
    }

    protected void validate(AbortMultipartUploadRequestBody input)  throws InvalidInputException {
        try {
            requireNonNull(input);
            requireNonNull(input.getUploadId());
            requireNonNull(input.getKey());
        } catch (Exception e) {
            throw new InvalidInputException(e);
        }
    }

    protected AbortMultipartUploadRequest toAbortMultipartUploadRequest(AbortMultipartUploadRequestBody input) {
        return new AbortMultipartUploadRequest(bucketName, input.getKey(), input.getUploadId());
    }

    @Override
    protected Integer getSuccessStatusCode(AbortMultipartUploadRequestBody input, SimpleMessageResponse output) {
        return SC_OK;
    }
}

