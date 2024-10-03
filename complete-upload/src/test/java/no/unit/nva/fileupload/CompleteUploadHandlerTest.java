package no.unit.nva.fileupload;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

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
    private static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";

    private CompleteUploadHandler completeUploadHandler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;

    /**
     * Setup test env.
     */
    @BeforeEach
    void setUp() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)).thenReturn(S3Constants.S3_UPLOAD_BUCKET_KEY);
        s3client = mock(AmazonS3Client.class);
        completeUploadHandler = new CompleteUploadHandler(environment, s3client, TEST_BUCKET_NAME);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @ParameterizedTest
    @ValueSource(strings = {"filename=\"filename.pdf\"", "", "filename=\"\"", "filename.pdf",
            "filename=\"Screenshot 2023-08-17 at 19.18.56.png\""})
    void canCompleteUpload(String filename) throws IOException {
        mockS3(filename);
        completeUploadHandler.handleRequest(completeUploadRequestWithBody(), outputStream, context);
        var response =
            GatewayResponse.fromOutputStream(outputStream, CompleteUploadResponseBody.class);
        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void completeUploadWithInvalidInputReturnsBadRequest() throws IOException {
        completeUploadHandler.handleRequest(completeUploadRequestWithoutBody(), outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_BAD_REQUEST)));
    }

    @Test
    void completeUploadWithS3ErrorReturnsNotFound() throws IOException {
        when(s3client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
                .thenThrow(AmazonS3Exception.class);

        completeUploadHandler.handleRequest(completeUploadRequestWithBody(), outputStream, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertThat(response, is(notNullValue()));
        assertThat(response.getStatusCode(), is(equalTo(SC_NOT_FOUND)));
        assertThat(response.getBody(), is(notNullValue()));
    }

    @Test
    void canCreateRequestWithEmptyElement() throws IOException {
        InputStream stream =
                CompleteUploadHandlerTest.class.getResourceAsStream(COMPLETE_UPLOAD_REQUEST_WITH_EMPTY_ELEMENT_JSON);
        final CompleteUploadRequestBody completeUploadRequestBody = dtoObjectMapper
                .readValue(new InputStreamReader(stream), CompleteUploadRequestBody.class);
        assertThat(completeUploadRequestBody, is(notNullValue()));

        final CompleteMultipartUploadRequest completeMultipartUploadRequest =
                completeUploadHandler.toCompleteMultipartUploadRequest(completeUploadRequestBody);
        assertThat(completeMultipartUploadRequest, is(notNullValue()));
        assertThat(completeUploadRequestBody.getParts(),
                not(hasSize(completeMultipartUploadRequest.getPartETags().size())));
    }

    @Test
    void canCreateRequestWithOnePart() throws IOException {
        InputStream stream =
                CompleteUploadHandlerTest.class.getResourceAsStream(COMPLETE_UPLOAD_REQUEST_WITH_ONE_PART_JSON);
        final CompleteUploadRequestBody completeUploadRequestBody = dtoObjectMapper
                .readValue(new InputStreamReader(stream), CompleteUploadRequestBody.class);
        assertThat(completeUploadRequestBody, is(notNullValue()));
        assertThat(completeUploadRequestBody.getParts(), is(notNullValue()));
        assertThat(completeUploadRequestBody.getParts(), hasSize(EXPECTED_ONE_PART));

        final CompleteMultipartUploadRequest completeMultipartUploadRequest =
                completeUploadHandler.toCompleteMultipartUploadRequest(completeUploadRequestBody);
        assertThat(completeMultipartUploadRequest, is(notNullValue()));
        assertThat(completeUploadRequestBody.getParts(), hasSize(completeMultipartUploadRequest.getPartETags().size()));
    }

    private void mockS3(String filename) {
        when(s3client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
            .thenReturn(new CompleteMultipartUploadResult());
        var s3object = new S3Object();
        s3object.setKey(randomString());
        var metadata = new ObjectMetadata();
        metadata.setContentLength(12345);
        metadata.setContentDisposition(filename);
        metadata.setContentType("application/pdf");
        s3object.setObjectMetadata(metadata);
        when(s3client.getObject(any())).thenReturn(s3object);
        when(s3client.getObjectMetadata(any())).thenReturn(metadata);
    }

    private InputStream completeUploadRequestWithBody() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<CompleteUploadRequestBody>(dtoObjectMapper)
                .withBody(completeUploadRequestBody())
                .build();
    }

    private InputStream completeUploadRequestWithoutBody() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<CompleteMultipartUploadRequest>(dtoObjectMapper)
                .build();
    }

    private CompleteUploadRequestBody completeUploadRequestBody() {
        List<CompleteUploadPart> partEtags = new ArrayList<>();
        partEtags.add(new CompleteUploadPart(1, "eTag1"));
        return new CompleteUploadRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY, partEtags);
    }

}
