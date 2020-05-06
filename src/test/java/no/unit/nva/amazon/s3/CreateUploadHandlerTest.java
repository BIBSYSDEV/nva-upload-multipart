package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import no.unit.nva.amazon.s3.model.CreateUploadRequestBody;
import no.unit.nva.amazon.s3.model.CreateUploadResponseBody;
import no.unit.nva.testutils.HandlerUtils;
import no.unit.nva.testutils.TestContext;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static no.unit.nva.amazon.s3.util.Environment.ALLOWED_ORIGIN_KEY;
import static no.unit.nva.amazon.s3.util.Environment.S3_UPLOAD_BUCKET_KEY;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"checkstyle:javadoctype", "checkstyle:MissingJavadocMethod"})
public class CreateUploadHandlerTest {

    public static final String SAMPLE_FILENAME = "filename";
    public static final String SAMPLE_MIMETYPE = "mime/type";
    public static final String SAMPLE_SIZE_STRING = "size";
    public static final String SAMPLE_MD5HASH = "md5hash";
    public static final String SAMPLE_UPLOADKEY = "uploadKey";
    public static final String SAMPLE_UPLOADID = "uploadId";
    public static final String TEST_BUCKET_NAME = "bucketName";

    private Environment environment;
    private CreateUploadHandler createUploadHandler;
    private HandlerUtils handlerUtils;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;

    /**
     * Setup test env.
     */
    @Before
    public void setUp() {
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_KEY)).thenReturn(ALLOWED_ORIGIN_KEY);
        when(environment.readEnv(S3_UPLOAD_BUCKET_KEY)).thenReturn(S3_UPLOAD_BUCKET_KEY);
        s3client = mock(AmazonS3Client.class);
        createUploadHandler = new CreateUploadHandler(environment, s3client, TEST_BUCKET_NAME);
        context = new TestContext();
        handlerUtils = new HandlerUtils(objectMapper);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void testHandleRequestMissingParameters() throws Exception {
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(null, null);

        createUploadHandler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CreateUploadResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testHandleRequest() throws IOException {
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenReturn(uploadResult());

        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(createUploadRequestBody(), null);
        createUploadHandler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CreateUploadResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        CreateUploadResponseBody responseBody = response.getBodyObject(CreateUploadResponseBody.class);
        assertNotNull(responseBody.getKey());
        assertNotNull(responseBody.getUploadId());
    }

    @Test
    public void testHandleFailingRequest() throws IOException {
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenThrow(SdkClientException.class);

        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(createUploadRequestBody(), null);
        createUploadHandler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CreateUploadResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestException() throws IOException {
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenThrow(RuntimeException.class);

        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(createUploadRequestBody(), null);
        createUploadHandler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CreateUploadResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestNoInput() throws IOException {
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(null, null);
        createUploadHandler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CreateUploadResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingFileparameters() throws IOException {
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(createUploadRequestBodyNoFilename(), null);
        createUploadHandler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<CreateUploadResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleGetObjectMetadata() {
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

    @Test
    public void testCreateUploadRequestConstructor() {
        CreateUploadRequestBody requestBody =
                new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        requestBody.setMd5hash(SAMPLE_MD5HASH);
        assertEquals(SAMPLE_FILENAME, requestBody.getFilename());
        assertEquals(SAMPLE_SIZE_STRING, requestBody.getSize());
        assertEquals(SAMPLE_MIMETYPE, requestBody.getMimetype());
        assertEquals(SAMPLE_MD5HASH, requestBody.getMd5hash());
    }

    protected CreateUploadRequestBody createUploadRequestBody() {
        return new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
    }

    protected CreateUploadRequestBody createUploadRequestBodyNoFilename() {
        return new CreateUploadRequestBody(null, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
    }

    protected InitiateMultipartUploadResult uploadResult() {
        InitiateMultipartUploadResult uploadResult =  new InitiateMultipartUploadResult();
        uploadResult.setKey(SAMPLE_UPLOADKEY);
        uploadResult.setUploadId(SAMPLE_UPLOADID);
        return uploadResult;
    }
}
