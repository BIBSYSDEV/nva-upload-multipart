package no.unit.nva.fileupload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CompleteUploadHandlerTest {

    private static final String COMPLETE_UPLOAD_REQUEST_WITH_EMPTY_ELEMENT_JSON
            = "/CompleteRequestWithEmptyElement.json";
    private static final String COMPLETE_UPLOAD_REQUEST_WITH_ONE_PART_JSON
            = "/CompleteRequestWithOnePart.json";
    public static final String TEST_BUCKET_NAME = "bucketName";
    public static final String SAMPLE_KEY = "key";
    public static final String SAMPLE_UPLOAD_ID = "uploadID";
    public static final String WILDCARD = "*";
    public static final int EXPECTED_ONE_PART = 1;

    private CompleteUploadHandler completeUploadHandler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;

    /**
     * Setup test env.
     */
    @Before
    public void setUp() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)).thenReturn(S3Constants.S3_UPLOAD_BUCKET_KEY);
        s3client = mock(AmazonS3Client.class);
        completeUploadHandler = new CompleteUploadHandler(environment, s3client, TEST_BUCKET_NAME);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void canCompleteUpload() throws IOException {
        when(s3client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
                .thenReturn(new CompleteMultipartUploadResult());

        completeUploadHandler.handleRequest(completeUploadRequestWithBody(), outputStream, context);
        GatewayResponse<CompleteUploadResponseBody> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void completeUploadWithInvalidInputReturnsBadRequest() throws IOException {
        completeUploadHandler.handleRequest(completeUploadRequestWithoutBody(), outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void completeUploadWithS3ErrorReturnsNotFound() throws IOException {
        when(s3client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
                .thenThrow(AmazonS3Exception.class);

        completeUploadHandler.handleRequest(completeUploadRequestWithBody(), outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);

        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void canCreateRequestWithEmptyElement() throws IOException {
        InputStream stream =
                CompleteUploadHandlerTest.class.getResourceAsStream(COMPLETE_UPLOAD_REQUEST_WITH_EMPTY_ELEMENT_JSON);
        final CompleteUploadRequestBody completeUploadRequestBody = objectMapper
                .readValue(new InputStreamReader(stream), CompleteUploadRequestBody.class);
        assertNotNull(completeUploadRequestBody);

        final CompleteMultipartUploadRequest completeMultipartUploadRequest =
                completeUploadHandler.toCompleteMultipartUploadRequest(completeUploadRequestBody);
        assertNotNull(completeMultipartUploadRequest);

        assertNotEquals(completeMultipartUploadRequest.getPartETags().size(),
                completeUploadRequestBody.getParts().size());
    }

    @Test
    public void canCreateRequestWithOnePart() throws IOException {
        InputStream stream =
                CompleteUploadHandlerTest.class.getResourceAsStream(COMPLETE_UPLOAD_REQUEST_WITH_ONE_PART_JSON);
        final CompleteUploadRequestBody completeUploadRequestBody = objectMapper
                .readValue(new InputStreamReader(stream), CompleteUploadRequestBody.class);
        assertNotNull(completeUploadRequestBody);
        assertNotNull(completeUploadRequestBody.getParts());
        assertEquals(EXPECTED_ONE_PART, completeUploadRequestBody.getParts().size());

        final CompleteMultipartUploadRequest completeMultipartUploadRequest =
                completeUploadHandler.toCompleteMultipartUploadRequest(completeUploadRequestBody);
        assertNotNull(completeMultipartUploadRequest);

        assertEquals(completeMultipartUploadRequest.getPartETags().size(),
                completeUploadRequestBody.getParts().size());
    }

    private InputStream completeUploadRequestWithBody() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<CompleteUploadRequestBody>(objectMapper)
                .withBody(completeUploadRequestBody())
                .build();
    }

    private InputStream completeUploadRequestWithoutBody() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<CompleteMultipartUploadRequest>(objectMapper)
                .build();
    }

    private CompleteUploadRequestBody completeUploadRequestBody() {
        List<CompleteUploadPart> partEtags = new ArrayList<>();
        partEtags.add(new CompleteUploadPart(1, "eTag1"));
        return new CompleteUploadRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY, partEtags);
    }

}
