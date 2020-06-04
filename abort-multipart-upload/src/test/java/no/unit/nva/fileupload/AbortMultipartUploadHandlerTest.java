package no.unit.nva.fileupload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.testutils.HandlerUtils;
import no.unit.nva.testutils.TestContext;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.GatewayResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbortMultipartUploadHandlerTest {

    public static final String TEST_BUCKET_NAME = "bucketName";
    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String SAMPLE_KEY = "key";
    public static final String WILDCARD = "*";

    private nva.commons.utils.Environment environment;
    private AbortMultipartUploadHandler abortMultipartUploadHandler;
    private HandlerUtils handlerUtils;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;

    /**
     * Setup test env.
     */
    @Before
    public void setUp() {
        environment = mock(nva.commons.utils.Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)).thenReturn(S3Constants.S3_UPLOAD_BUCKET_KEY);
        s3client = mock(AmazonS3Client.class);
        abortMultipartUploadHandler = new AbortMultipartUploadHandler(environment, s3client, TEST_BUCKET_NAME);
        context = new TestContext();
        handlerUtils = new HandlerUtils(objectMapper);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void canAbortMultipartUpload() throws IOException {
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(abortMultipartUploadRequestBody(), null);
        abortMultipartUploadHandler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<SimpleMessageResponse> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);
        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void abortMultipartUploadWithInvalidInputReturnsBadRequest() throws IOException {
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(null, null);
        abortMultipartUploadHandler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void abortMultipartUploadWithS3ErrorReturnsNotFound() throws IOException {
        doThrow(AmazonS3Exception.class).when(s3client).abortMultipartUpload(Mockito.any());

        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(abortMultipartUploadRequestBody(), null);
        abortMultipartUploadHandler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    private AbortMultipartUploadRequestBody abortMultipartUploadRequestBody() {
        return new AbortMultipartUploadRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);
    }
}
