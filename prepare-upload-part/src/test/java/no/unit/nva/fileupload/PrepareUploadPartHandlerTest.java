package no.unit.nva.fileupload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.testutils.HandlerUtils;
import no.unit.nva.testutils.TestContext;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"checkstyle:javadoctype", "checkstyle:MissingJavadocMethod"})
public class PrepareUploadPartHandlerTest {

    public static final String SAMPLE_KEY = "key";
    public static final String SAMPLE_UPLOADID = "uploadId";
    public static final String SAMPLE_BODY = "body";
    public static final String SAMPLE_PART_NUMBER = "1";
    public static final String TEST_BUCKET_NAME = "bucketName";
    public static final String WILDCARD = "*";

    private Environment environment;
    private PrepareUploadPartHandler prepareUploadPartHandler;
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
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)).thenReturn(S3Constants.S3_UPLOAD_BUCKET_KEY);
        s3client = mock(AmazonS3Client.class);
        prepareUploadPartHandler = new PrepareUploadPartHandler(environment, s3client, TEST_BUCKET_NAME);
        context = new TestContext();
        handlerUtils = new HandlerUtils(objectMapper);
        outputStream = new ByteArrayOutputStream();
    }

    private PrepareUploadPartRequestBody prepareUploadPartRequestBody() {
        PrepareUploadPartRequestBody requestInputBody =
                new PrepareUploadPartRequestBody(SAMPLE_UPLOADID, SAMPLE_KEY, SAMPLE_BODY, SAMPLE_PART_NUMBER);
        return requestInputBody;
    }

    @Test
    public void testHandleRequestMissingParameters() throws IOException {
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(null, null);
        prepareUploadPartHandler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<PrepareUploadPartResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testHandleRequest() throws IOException {
        URL dummyUrl = new URL("http://localhost");
        when(s3client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class))).thenReturn(dummyUrl);

        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(prepareUploadPartRequestBody(), null);
        prepareUploadPartHandler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<PrepareUploadPartResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
        PrepareUploadPartResponseBody responseBody = response.getBodyObject(PrepareUploadPartResponseBody.class);
        assertNotNull(responseBody.getUrl());
    }

    @Test
    public void testHandleFailingRequest() throws IOException {
        when(s3client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(AmazonS3Exception.class);

        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(prepareUploadPartRequestBody(), null);
        prepareUploadPartHandler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<PrepareUploadPartResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    public void testHandleFailingRequestMissingInputParameters() throws IOException {
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(null, null);
        prepareUploadPartHandler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<PrepareUploadPartResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testPrepareUploadRequestBodyConstructor() {
        PrepareUploadPartRequestBody requestInputBody =
                new PrepareUploadPartRequestBody(SAMPLE_UPLOADID, SAMPLE_KEY, SAMPLE_BODY, SAMPLE_PART_NUMBER);
        assertEquals(SAMPLE_UPLOADID, requestInputBody.getUploadId());
        assertEquals(SAMPLE_KEY, requestInputBody.getKey());
        assertEquals(SAMPLE_BODY, requestInputBody.getBody());
        assertEquals(SAMPLE_PART_NUMBER, requestInputBody.getNumber());
    }

}
