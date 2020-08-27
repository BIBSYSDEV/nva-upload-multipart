package no.unit.nva.fileupload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import no.unit.nva.fileupload.exception.InvalidInputException;
import no.unit.nva.fileupload.model.CreateUploadRequestBody;
import no.unit.nva.fileupload.model.CreateUploadResponseBody;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.fileupload.util.S3Utils;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.apache.commons.text.translate.UnicodeEscaper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpStatus.SC_CREATED;

public class CreateUploadHandler extends ApiGatewayHandler<CreateUploadRequestBody, CreateUploadResponseBody> {

    private static final Logger logger = LoggerFactory.getLogger(CreateUploadHandler.class);
    public static final String CONTENT_DISPOSITION_TEMPLATE = "filename=\"%s\"";
    public static final int LAST_ASCII_CODEPOINT = 127;
    private final transient AmazonS3 s3Client;
    private final transient String bucketName;

    /**
     * Default constructor for CreateUploadHandler.
     */
    @JacocoGenerated
    public CreateUploadHandler() {
        this(new Environment());
    }

    /**
     * Constructor for CreateUploadHandler.
     *
     * @param environment environment reader
     */
    @JacocoGenerated
    public CreateUploadHandler(Environment environment) {
        this(
                environment,
                S3Utils.createAmazonS3Client(environment.readEnv(S3Constants.AWS_REGION_KEY)),
                environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)
        );
    }

    /**
     * Construct for lambda eventhandler to create an upload request for S3.
     */
    public CreateUploadHandler(Environment environment, AmazonS3 s3Client, String bucketName) {
        super(CreateUploadRequestBody.class, environment, logger);
        this.bucketName = bucketName;
        this.s3Client = s3Client;

    }

    /**
     * Extracting metadata from the given file resource.
     * @param requestBody incoming parameters
     * @return Metadata to be stored with file on S3
     */
    protected ObjectMetadata toObjectMetadata(CreateUploadRequestBody requestBody) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentMD5(null);
        objectMetadata.setContentDisposition(toContentDisposition(requestBody));
        objectMetadata.setContentType(requestBody.getMimetype());
        return objectMetadata;
    }

    private String getEscapedFilename(CreateUploadRequestBody requestBody) {
        UnicodeEscaper unicodeEscaper = UnicodeEscaper.above(LAST_ASCII_CODEPOINT);
        return unicodeEscaper.translate(requestBody.getFilename());
    }

    private String toContentDisposition(CreateUploadRequestBody requestBody) {
        return String.format(CONTENT_DISPOSITION_TEMPLATE, getEscapedFilename(requestBody));
    }

    @Override
    protected CreateUploadResponseBody processInput(CreateUploadRequestBody input, RequestInfo requestInfo,
                                                    Context context) throws ApiGatewayException {
        validate(input);

        String keyName = UUID.randomUUID().toString();
        InitiateMultipartUploadRequest initRequest =
                new InitiateMultipartUploadRequest(bucketName, keyName, toObjectMetadata(input));
        InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

        return new CreateUploadResponseBody(initResponse.getUploadId(), keyName);
    }

    private void validate(CreateUploadRequestBody input) throws InvalidInputException {
        try {
            requireNonNull(input);
            requireNonNull(input.getFilename());
            requireNonNull(input.getSize());
            MediaType.valueOf(input.getMimetype());
        } catch (Exception e) {
            logger.warn(e.getMessage());
            throw new InvalidInputException(e);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(CreateUploadRequestBody input, CreateUploadResponseBody output) {
        return SC_CREATED;
    }



}