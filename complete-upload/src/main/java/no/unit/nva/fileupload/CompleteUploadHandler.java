package no.unit.nva.fileupload;

import static java.util.Objects.requireNonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.HttpStatus.SC_OK;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import no.unit.nva.fileupload.exception.InvalidInputException;
import no.unit.nva.fileupload.exception.NotFoundException;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.fileupload.util.S3Utils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompleteUploadHandler extends ApiGatewayHandler<CompleteUploadRequestBody, CompleteUploadResponseBody> {

    private static final Logger logger = LoggerFactory.getLogger(CompleteUploadHandler.class);
    public static final String S3_ERROR = "S3 error";
    public static final String FILE_NAME_REGEX = "filename=\"(.*)\"";

    // The total time the SDK will wait for the entire request execution, including retries
    public static final int SDK_CLIENT_EXECUTION_TIMEOUT_MS = 8 * 1000; // 8 seconds, enough for 3 retries of 2 seconds

    // The time the SDK will wait for data transfer for a single request.
    public static final int SDK_REQUEST_TIMEOUT_MS = 2 * 1000; // 2 seconds

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
                S3Utils.createAmazonS3Client(environment.readEnv(S3Constants.AWS_REGION_KEY)),
                environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)
        );
    }

    /**
     * Construct for lambda event handler to create an upload request for S3.
     */
    public CompleteUploadHandler(Environment environment, AmazonS3 s3Client, String bucketName) {
        super(CompleteUploadRequestBody.class, environment);
        this.bucketName = bucketName;
        this.s3Client = s3Client;
    }

    @Override
    protected void validateRequest(CompleteUploadRequestBody completeUploadRequestBody, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        validate(completeUploadRequestBody);
    }

    @Override
    protected CompleteUploadResponseBody processInput(CompleteUploadRequestBody input, RequestInfo requestInfo,
                                                      Context context) throws ApiGatewayException {

        return attempt(() -> toCompleteMultipartUploadRequest(input))
                   .map(this::completeMultipartUpload)
                   .map(this::toCompletedUploadResponseBody)
                   .orElseThrow(CompleteUploadHandler::handleFailure);
    }

    private static  ApiGatewayException handleFailure(
        Failure<CompleteUploadResponseBody> failure) {
        var exception = failure.getException();
        return exception instanceof NotFoundException
                   ? new nva.commons.apigateway.exceptions.NotFoundException(exception.getMessage())
                   : (ApiGatewayException) exception;
    }

    private CompleteUploadResponseBody toCompletedUploadResponseBody(CompleteResult result) {
        var metadata = result.getS3ObjectMetadata();
        return new CompleteUploadResponseBody.Builder()
                   .withSize(metadata.getContentLength())
                   .withLocation(result.getKey())
                   .withMimeType(metadata.getContentType())
                   .withFileName(toFileName(metadata.getContentDisposition()))
                   .build();
    }

    private String toFileName(String contentDisposition) {
        var pattern = Pattern.compile(FILE_NAME_REGEX);
        var matcher = pattern.matcher(contentDisposition);
        return matcher.matches() ? matcher.group(1) : contentDisposition;
    }

    /**
     * Extracts and checks requestdata into s3 understandable stuff.
     * @param requestBody Request from frontend
     * @return request to send to S3
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected CompleteMultipartUploadRequest toCompleteMultipartUploadRequest(CompleteUploadRequestBody requestBody) {
        CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest();
        completeMultipartUploadRequest.setBucketName(bucketName);
        completeMultipartUploadRequest.setKey(requestBody.getKey());
        completeMultipartUploadRequest.setUploadId(requestBody.getUploadId());
        completeMultipartUploadRequest.setSdkClientExecutionTimeout(SDK_CLIENT_EXECUTION_TIMEOUT_MS);
        completeMultipartUploadRequest.setSdkRequestTimeout(SDK_REQUEST_TIMEOUT_MS);

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

    private CompleteResult completeMultipartUpload(
            CompleteMultipartUploadRequest completeMultipartUploadRequest)
        throws NotFoundException, JsonProcessingException {
        try {
            logger.info("Completing multipart upload...");
            var result = s3Client.completeMultipartUpload(completeMultipartUploadRequest);
            logger.info("Completed multipart upload");
            logger.info(dtoObjectMapper.writeValueAsString(result));
            var request = new GetObjectMetadataRequest(bucketName, result.getKey());
            request.setSdkRequestTimeout(SDK_REQUEST_TIMEOUT_MS);
            request.setSdkClientExecutionTimeout(SDK_CLIENT_EXECUTION_TIMEOUT_MS);
            return new CompleteResult(result.getKey(), s3Client.getObjectMetadata(request));
        } catch (AmazonS3Exception e) {
            logger.warn(e.getMessage());
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

    private static class CompleteResult {
        private final String key;
        private final ObjectMetadata objectMetadata;

        public CompleteResult(String key, ObjectMetadata s3Object) {
            this.key = key;
            this.objectMetadata = s3Object;
        }

        public String getKey() {
            return key;
        }

        public ObjectMetadata getS3ObjectMetadata() {
            return objectMetadata;
        }
    }
}