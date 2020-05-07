package no.unit.nva.amazon.s3;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import no.unit.nva.amazon.s3.exception.InvalidInputException;
import no.unit.nva.amazon.s3.exception.NotFoundException;
import no.unit.nva.amazon.s3.model.CompleteUploadPart;
import no.unit.nva.amazon.s3.model.CompleteUploadRequestBody;
import no.unit.nva.amazon.s3.model.CompleteUploadResponseBody;
import no.unit.nva.amazon.s3.util.S3Constants;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static no.unit.nva.amazon.s3.util.S3Constants.AWS_REGION_KEY;
import static no.unit.nva.amazon.s3.util.S3Utils.createAmazonS3Client;
import static org.apache.http.HttpStatus.SC_OK;

public class CompleteUploadHandler extends ApiGatewayHandler<CompleteUploadRequestBody, CompleteUploadResponseBody> {

    private static final Logger logger = LoggerFactory.getLogger(CompleteUploadHandler.class);
    public static final String S3_ERROR = "S3 error";

    private final transient String bucketName;
    private final transient AmazonS3 s3Client;

    /**
     * Default constructor for CompleteUploadHandler.
     */
    @JacocoGenerated
    public CompleteUploadHandler() {
        this(new Environment());
    }

    /**
     * Constructor for CompleteUploadHandler.
     *
     * @param environment   environment reader
     */
    @JacocoGenerated
    public CompleteUploadHandler(Environment environment) {
        this(
                environment,
                createAmazonS3Client(environment.readEnv(AWS_REGION_KEY)),
                environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)
        );
    }

    /**
     * Construct for lambda event handler to create an upload request for S3.
     */
    public CompleteUploadHandler(Environment environment, AmazonS3 s3Client, String bucketName) {
        super(CompleteUploadRequestBody.class, environment, logger);
        this.bucketName = bucketName;
        this.s3Client = s3Client;
    }

    /**
     * Extracts and checks requestdata into s3 understandable stuff.
     * @param requestBody Request from frontend
     * @return request to send to S3
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public CompleteMultipartUploadRequest toCompleteMultipartUploadRequest(CompleteUploadRequestBody requestBody) {
        CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest();
        completeMultipartUploadRequest.setBucketName(bucketName);
        completeMultipartUploadRequest.setKey(requestBody.getKey());
        completeMultipartUploadRequest.setUploadId(requestBody.getUploadId());

        List<PartETag> partETags = requestBody.getParts().stream()
                .filter(CompleteUploadPart::hasValue)
                .map(this::toPartETag)
                .collect(Collectors.toList());

        completeMultipartUploadRequest.setPartETags(partETags);
        return completeMultipartUploadRequest;
    }

    private PartETag toPartETag(CompleteUploadPart completeUploadPart) {
        return new PartETag(completeUploadPart.getPartNumber(), completeUploadPart.getEtag());
    }

    @Override
    protected CompleteUploadResponseBody processInput(CompleteUploadRequestBody input, RequestInfo requestInfo,
                                                      Context context) throws ApiGatewayException {
        validate(input);
        CompleteMultipartUploadResult uploadResult = completeMultipartUpload(toCompleteMultipartUploadRequest(input));
        return new CompleteUploadResponseBody(uploadResult.getKey());
    }

    private CompleteMultipartUploadResult completeMultipartUpload(
            CompleteMultipartUploadRequest completeMultipartUploadRequest) throws NotFoundException {
        try {
            return s3Client.completeMultipartUpload(completeMultipartUploadRequest);
        } catch (AmazonS3Exception e) {
            throw new NotFoundException(S3_ERROR, e);
        }
    }

    private void validate(CompleteUploadRequestBody input) throws InvalidInputException {
        try {
            requireNonNull(input);
            requireNonNull(input.getUploadId());
            requireNonNull(input.getKey());
            requireNonNull(input.getParts());
        } catch (Exception e) {
            throw new InvalidInputException(e);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(CompleteUploadRequestBody input, CompleteUploadResponseBody output) {
        return SC_OK;
    }
}