package no.unit.nva.fileupload;

import static no.unit.nva.fileupload.CreateUploadHandler.CONTENT_DISPOSITION_TEMPLATE;
import static nva.commons.core.JsonUtils.dtoObjectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import no.unit.nva.fileupload.model.CreateUploadRequestBody;
import no.unit.nva.fileupload.model.CreateUploadResponseBody;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.zalando.problem.Problem;

public class CreateUploadHandlerTest {

    public static final String SAMPLE_FILENAME = "filename";
    public static final String SAMPLE_MIMETYPE = "mime/type";
    public static final String SAMPLE_SIZE_STRING = "222";
    public static final String SAMPLE_UPLOAD_KEY = "uploadKey";
    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String TEST_BUCKET_NAME = "bucketName";
    public static final String WILDCARD = "*";
    public static final String SAMPLE_UNICODE_FILENAME = "normal_üñīḉøđḝ_ƒıļæ_ňåɱë";
    public static final String EXPECTED_ESCAPED_FILENAME = "normal_\\u00FC\\u00F1\\u012B\\u1E09\\u00F8\\u0111\\u1E1D_"
                                                           + "\\u0192\\u0131\\u013C\\u00E6_\\u0148\\u00E5\\u0271"
                                                           + "\\u00EB";
    public static final String EMPTY_STRING = "";
    public static final String INVALID_MIME_TYPE = "meme/type";
    public static final String NULL_STRING = "null";

    private CreateUploadHandler createUploadHandler;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;
    private ObjectMapper objectMapper = dtoObjectMapper;

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
        var actualBody = actual.getBodyObject(CreateUploadResponseBody.class);
        var expectedBody = new CreateUploadResponseBody(SAMPLE_UPLOAD_ID, getGeneratedKey(actual));
        assertThat(actualBody, is(equalTo(expectedBody)));
        assertThat(actual.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
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
    public void createUploadRequestBodyReturnsValidContentDispositionWhenInputIsValid() {
        CreateUploadRequestBody requestBody =
            new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        String actual = createUploadHandler.toObjectMetadata(requestBody).getContentDisposition();
        String expected = generateContentDisposition(SAMPLE_FILENAME);
        assertEquals(expected, actual);
    }

    @Test
    public void createUploadRequestBodyReturnsValidContentDispositionWhenFilenameIsNull() {
        CreateUploadRequestBody requestBody =
            new CreateUploadRequestBody(null, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        String actual = createUploadHandler.toObjectMetadata(requestBody).getContentDisposition();
        String expected = generateContentDisposition(NULL_STRING);
        assertEquals(expected, actual);
    }

    @Test
    public void createUploadRequestBodyReturnsValidContentDispositionWhenFilenameIsEmptyString() {
        CreateUploadRequestBody requestBody =
            new CreateUploadRequestBody(EMPTY_STRING, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        String actual = createUploadHandler.toObjectMetadata(requestBody).getContentDisposition();
        String expected = generateContentDisposition(EMPTY_STRING);
        assertEquals(expected, actual);
    }

    @Test
    public void createUploadRequestBodyReturnsNullContentTypeWhenMimeTypeIsNull() {
        CreateUploadRequestBody requestBody =
            new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, null);
        String actual = createUploadHandler.toObjectMetadata(requestBody).getContentType();
        assertNull(actual);
    }

    @Test
    public void createUploadRequestBodyReturnsEmptyStringContentTypeWhenMimeTypeIsEmptyString() {
        CreateUploadRequestBody requestBody =
            new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, EMPTY_STRING);
        String actual = createUploadHandler.toObjectMetadata(requestBody).getContentType();
        assertEquals(EMPTY_STRING, actual);
    }

    @Test
    public void createUploadRequestBodyReturnsContentTypeWhenMimeTypeIsInvalidString() {
        CreateUploadRequestBody requestBody =
            new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, INVALID_MIME_TYPE);
        String actual = createUploadHandler.toObjectMetadata(requestBody).getContentType();
        assertEquals(INVALID_MIME_TYPE, actual);
    }

    @Test
    public void createUploadRequestReturnsValidContentDispositionWithEscapedUnicodeWhenInputIsUnicode() {
        CreateUploadRequestBody requestBody =
            new CreateUploadRequestBody(SAMPLE_UNICODE_FILENAME, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
        ObjectMetadata objectMetadata = createUploadHandler.toObjectMetadata(requestBody);
        String actual = objectMetadata.getContentDisposition();
        String expected = generateContentDisposition(EXPECTED_ESCAPED_FILENAME);
        assertEquals(expected, actual);
    }

    // We get the key from the actual response because it was randomly generated
    protected String getGeneratedKey(GatewayResponse<CreateUploadResponseBody> actual)
        throws com.fasterxml.jackson.core.JsonProcessingException {
        return actual.getBodyObject(CreateUploadResponseBody.class).getKey();
    }

    protected CreateUploadRequestBody createUploadRequestBody() {
        return new CreateUploadRequestBody(SAMPLE_FILENAME, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
    }

    protected CreateUploadRequestBody createUploadRequestBodyNoFilename() {
        return new CreateUploadRequestBody(null, SAMPLE_SIZE_STRING, SAMPLE_MIMETYPE);
    }

    protected InitiateMultipartUploadResult uploadResult() {
        InitiateMultipartUploadResult uploadResult = new InitiateMultipartUploadResult();
        uploadResult.setKey(SAMPLE_UPLOAD_KEY);
        uploadResult.setUploadId(SAMPLE_UPLOAD_ID);
        return uploadResult;
    }

    private String generateContentDisposition(String filename) {
        return String.format(CONTENT_DISPOSITION_TEMPLATE, filename);
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
}
