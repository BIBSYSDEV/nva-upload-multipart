package no.unit.nva.fileupload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
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

    private AbortMultipartUploadHandler abortMultipartUploadHandler;
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
        abortMultipartUploadHandler = new AbortMultipartUploadHandler(environment, s3client, TEST_BUCKET_NAME);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void canAbortMultipartUpload() throws IOException {
        abortMultipartUploadHandler.handleRequest(abortMultipartUploadRequestWithBody(), outputStream, context);

        GatewayResponse<SimpleMessageResponse> response = GatewayResponse.fromOutputStream(outputStream);
        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void abortMultipartUploadWithInvalidInputReturnsBadRequest() throws IOException {
        abortMultipartUploadHandler.handleRequest(abortMultipartUploadRequestWithoutBody(), outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void abortMultipartUploadWithS3ErrorReturnsNotFound() throws IOException {
        doThrow(AmazonS3Exception.class).when(s3client).abortMultipartUpload(Mockito.any());
        abortMultipartUploadHandler.handleRequest(abortMultipartUploadRequestWithBody(), outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    private InputStream abortMultipartUploadRequestWithBody()
            throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<AbortMultipartUploadRequestBody>(objectMapper)
                .withBody(abortMultipartUploadRequestBody())
                .build();
    }

    private InputStream abortMultipartUploadRequestWithoutBody()
            throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<AbortMultipartUploadRequestBody>(objectMapper)
                .build();
    }

    private AbortMultipartUploadRequestBody abortMultipartUploadRequestBody() {
        return new AbortMultipartUploadRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);
    }
}
