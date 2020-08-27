package no.unit.nva.fileupload;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import no.unit.nva.fileupload.model.CreateUploadRequestBody;
import no.unit.nva.fileupload.model.CreateUploadResponseBody;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.junit.Before;
import org.junit.Test;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateUploadHandlerTest {

    public static final String SAMPLE_FILENAME = "filename";
    public static final String SAMPLE_MIMETYPE = "mime/type";
    public static final String SAMPLE_SIZE_STRING = "size";
    public static final String SAMPLE_UPLOAD_KEY = "uploadKey";
    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String TEST_BUCKET_NAME = "bucketName";
    public static final String WILDCARD = "*";

    private CreateUploadHandler createUploadHandler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;

    /**
     * Setup test env.
     */
    @Before
    public void setUp() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)).thenReturn(S3Constants.S3_UPLOAD_BUCKET_KEY);
        s3client = mock(AmazonS3Client.class);
        createUploadHandler = new CreateUploadHandler(environment, s3client, TEST_BUCKET_NAME);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void canCreateUpload() throws Exception {
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenReturn(uploadResult());

        createUploadHandler.handleRequest(
                createUploadRequestWithBody(createUploadRequestBody()), outputStream, context);

        GatewayResponse<CreateUploadResponseBody> actual = GatewayResponse.fromOutputStream(outputStream);

        GatewayResponse<CreateUploadResponseBody> expected = new GatewayResponse<>(
            new CreateUploadResponseBody(SAMPLE_UPLOAD_ID, getGeneratedKey(actual)),
            TestHeaders.getResponseHeaders(),
            SC_CREATED
        );

        assertEquals(expected, actual);
    }

    // We get the key from the actual response because it was randomly generated
    protected String getGeneratedKey(GatewayResponse<CreateUploadResponseBody> actual)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        return actual.getBodyObject(CreateUploadResponseBody.class).getKey();
    }

    @Test
    public void createUploadWithInvalidInputReturnBadRequest() throws Exception {
        createUploadHandler.handleRequest(createUploadRequestWithoutBody(), outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void createUploadWithS3ErrorReturnsNotFound() throws IOException {
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenThrow(SdkClientException.class);

        createUploadHandler.handleRequest(
                createUploadRequestWithBody(createUploadRequestBody()), outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void createUploadWithRuntimeErrorReturnsServerError() throws IOException {
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenThrow(RuntimeException.class);

        createUploadHandler.handleRequest(
                createUploadRequestWithBody(createUploadRequestBody()), outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void setCreateUploadHandlerWithMissingFileParametersReturnsBadRequest() throws IOException {
        createUploadHandler.handleRequest(
                createUploadRequestWithBody(createUploadRequestBodyNoFilename()), outputStream, context);

        GatewayResponse<CreateUploadResponseBody> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void canCreateObjectMetadataFromInput() {
        CreateUploadRequestBody requestBody =
                new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        ObjectMetadata objectMetadata = createUploadHandler.toObjectMetadata(requestBody);
        assertNotNull(objectMetadata);
        requestBody = new CreateUploadRequestBody(null, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        objectMetadata = createUploadHandler.toObjectMetadata(requestBody);
        assertNotNull(objectMetadata);
        requestBody = new CreateUploadRequestBody("", SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        objectMetadata = createUploadHandler.toObjectMetadata(requestBody);
        assertNotNull(objectMetadata);
        requestBody = new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, null);
        objectMetadata = createUploadHandler.toObjectMetadata(requestBody);
        assertNotNull(objectMetadata);
        requestBody = new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, "");
        objectMetadata = createUploadHandler.toObjectMetadata(requestBody);
        assertNotNull(objectMetadata);
        requestBody = new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, "meme/type");
        objectMetadata = createUploadHandler.toObjectMetadata(requestBody);
        assertNotNull(objectMetadata.getContentType());
    }

    private InputStream createUploadRequestWithBody(CreateUploadRequestBody uploadRequestBody)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<CreateUploadRequestBody>(objectMapper)
                .withBody(uploadRequestBody)
                .build();
    }

    private InputStream createUploadRequestWithoutBody() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<CreateUploadRequestBody>(objectMapper)
                .build();
    }

    protected CreateUploadRequestBody createUploadRequestBody() {
        return new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
    }

    protected CreateUploadRequestBody createUploadRequestBodyNoFilename() {
        return new CreateUploadRequestBody(null, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
    }

    protected InitiateMultipartUploadResult uploadResult() {
        InitiateMultipartUploadResult uploadResult =  new InitiateMultipartUploadResult();
        uploadResult.setKey(SAMPLE_UPLOAD_KEY);
        uploadResult.setUploadId(SAMPLE_UPLOAD_ID);
        return uploadResult;
    }
}
