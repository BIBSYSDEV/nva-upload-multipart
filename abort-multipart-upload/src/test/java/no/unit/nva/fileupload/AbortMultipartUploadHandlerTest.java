package no.unit.nva.fileupload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
    @BeforeEach
    void setUp() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)).thenReturn(S3Constants.S3_UPLOAD_BUCKET_KEY);
        s3client = mock(AmazonS3Client.class);
        abortMultipartUploadHandler = new AbortMultipartUploadHandler(environment, s3client, TEST_BUCKET_NAME);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void canAbortMultipartUpload() throws IOException {
        abortMultipartUploadHandler.handleRequest(abortMultipartUploadRequestWithBody(), outputStream, context);

        GatewayResponse<SimpleMessageResponse> response = GatewayResponse.fromOutputStream(outputStream,
                                                                                           SimpleMessageResponse.class);
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void abortMultipartUploadWithInvalidInputReturnsBadRequest() throws IOException {
        abortMultipartUploadHandler.handleRequest(abortMultipartUploadRequestWithoutBody(), outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_BAD_REQUEST)));
    }

    @Test
    void abortMultipartUploadWithS3ErrorReturnsNotFound() throws IOException {
        doThrow(AmazonS3Exception.class).when(s3client).abortMultipartUpload(Mockito.any());
        abortMultipartUploadHandler.handleRequest(abortMultipartUploadRequestWithBody(), outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(SC_NOT_FOUND)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    private InputStream abortMultipartUploadRequestWithBody()
            throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<AbortMultipartUploadRequestBody>(dtoObjectMapper)
                .withBody(abortMultipartUploadRequestBody())
                .build();
    }

    private InputStream abortMultipartUploadRequestWithoutBody()
            throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<AbortMultipartUploadRequestBody>(dtoObjectMapper)
                .build();
    }

    private AbortMultipartUploadRequestBody abortMultipartUploadRequestBody() {
        return new AbortMultipartUploadRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);
    }
}
