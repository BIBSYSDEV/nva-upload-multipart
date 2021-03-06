package no.unit.nva.fileupload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
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
import java.net.URL;

import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrepareUploadPartHandlerTest {

    public static final String SAMPLE_KEY = "key";
    public static final String SAMPLE_UPLOADID = "uploadId";
    public static final String SAMPLE_BODY = "body";
    public static final String SAMPLE_PART_NUMBER = "1";
    public static final String TEST_BUCKET_NAME = "bucketName";
    public static final String WILDCARD = "*";

    private PrepareUploadPartHandler prepareUploadPartHandler;
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
        prepareUploadPartHandler = new PrepareUploadPartHandler(environment, s3client, TEST_BUCKET_NAME);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void canPrepareUploadPart() throws IOException {
        URL dummyUrl = new URL("http://localhost");
        when(s3client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class))).thenReturn(dummyUrl);

        prepareUploadPartHandler.handleRequest(prepareUploadPartRequestWithBody(), outputStream, context);
        GatewayResponse<PrepareUploadPartResponseBody> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
        PrepareUploadPartResponseBody responseBody = response.getBodyObject(PrepareUploadPartResponseBody.class);
        assertNotNull(responseBody.getUrl());
    }

    @Test
    public void prepareUploadPartWithInvalidInputReturnsBadRequest() throws IOException {
        prepareUploadPartHandler.handleRequest(prepareUploadPartRequestWithoutBody(), outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);

        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void prepareUploadPartWithS3ErrorReturnsNotFound() throws IOException {
        when(s3client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(AmazonS3Exception.class);

        prepareUploadPartHandler.handleRequest(prepareUploadPartRequestWithBody(), outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    private InputStream prepareUploadPartRequestWithBody() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<PrepareUploadPartRequestBody>(objectMapper)
                .withBody(prepareUploadPartRequestBody())
                .build();
    }

    private InputStream prepareUploadPartRequestWithoutBody()
            throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<PrepareUploadPartRequestBody>(objectMapper)
                .build();
    }

    private PrepareUploadPartRequestBody prepareUploadPartRequestBody() {
        return new PrepareUploadPartRequestBody(SAMPLE_UPLOADID, SAMPLE_KEY, SAMPLE_BODY, SAMPLE_PART_NUMBER);
    }
}
