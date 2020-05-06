package no.unit.nva.amazon.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.google.gson.Gson;
import no.unit.nva.amazon.s3.exception.ParameterMissingException;
import no.unit.nva.amazon.s3.model.AbortMultipartUploadRequestBody;
import no.unit.nva.amazon.s3.model.GatewayResponse;
import no.unit.nva.amazon.s3.util.Environment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static no.unit.nva.amazon.s3.model.GatewayResponse.BODY_KEY;
import static no.unit.nva.amazon.s3.util.Environment.ALLOWED_ORIGIN_KEY;
import static no.unit.nva.amazon.s3.util.Environment.S3_UPLOAD_BUCKET_KEY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"checkstyle:javadoctype", "checkstyle:MissingJavadocMethod"})
public class AbortMultipartUploadHandlerTest {

    public static final String BUCKET_NAME = "bucket";
    private Environment environment;

    /**
     * Setup test env.
     */
    @Before
    public void setUp() {
        environment = mock(Environment.class);
        Mockito.when(environment.get(ALLOWED_ORIGIN_KEY)).thenReturn(Optional.of(ALLOWED_ORIGIN_KEY));
        Mockito.when(environment.get(S3_UPLOAD_BUCKET_KEY)).thenReturn(Optional.of(S3_UPLOAD_BUCKET_KEY));
    }

    @Rule
    public final EnvironmentVariables environmentVariables  = new EnvironmentVariables();

    @Test
    public void testDefaultConstructor() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY,ALLOWED_ORIGIN_KEY);
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY,S3_UPLOAD_BUCKET_KEY);
        environmentVariables.set(Environment.AWS_REGION_KEY, Environment.DEFAULT_AWS_REGION);
        AbortMultipartUploadHandler abortMultipartUploadHandler = new AbortMultipartUploadHandler();
        assertNotNull(abortMultipartUploadHandler);
    }

    @Test
    public void testHandleRequestMissingParameters() {
        Map<String, Object> requestInput = new HashMap<>();
        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        AbortMultipartUploadHandler abortMultipartUploadHandler =
                new AbortMultipartUploadHandler(environment, mockS3Client);
        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);

        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testHandleRequest() {

        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));
        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        AbortMultipartUploadHandler abortMultipartUploadHandler =
                new AbortMultipartUploadHandler(environment, mockS3Client);
        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);
        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    private AbortMultipartUploadRequestBody createAbortMultipartUploadRequestBody() {
        AbortMultipartUploadRequestBody requestInputBody =
                new AbortMultipartUploadRequestBody("uploadId", "key");
        return requestInputBody;
    }

    @Test
    public void testHandleFailingRequest() {
        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));
        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        AmazonS3Exception amazonS3Exception = new AmazonS3Exception("mock-exception");
        AbortMultipartUploadHandler abortMultipartUploadHandler =
                new AbortMultipartUploadHandler(environment, mockS3Client);
        doThrow(amazonS3Exception).when(mockS3Client).abortMultipartUpload(Mockito.any());
        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);
        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestNoInput() {
        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        ParameterMissingException parameterMissingException = new ParameterMissingException("mock-exception");
        AbortMultipartUploadHandler abortMultipartUploadHandler =
                new AbortMultipartUploadHandler(environment, null);
        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(null, null);
        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingParameterUploadId() {
        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();
        requestInputBody.setUploadId(null);
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));
        AbortMultipartUploadHandler abortMultipartUploadHandler =
                new AbortMultipartUploadHandler(environment, null);
        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);
        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingParameterKey() {
        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();
        requestInputBody.setKey(null);
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));
        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        AbortMultipartUploadHandler abortMultipartUploadHandler =
                new AbortMultipartUploadHandler(environment, mockS3Client);
        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);
        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestCheckParametersOtherException() {
        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));
        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        Exception unmappedRuntimeException  = new RuntimeException("unmapped-mock-exception");
        AbortMultipartUploadHandler abortMultipartUploadHandler =
                Mockito.spy(new AbortMultipartUploadHandler(environment, mockS3Client));
        doThrow(unmappedRuntimeException).when(abortMultipartUploadHandler).checkParameters(Mockito.anyMap());
        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);
        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestOtherException() {
        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));
        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        Exception otherException = new RuntimeException("mock-jan-exception");
        AbortMultipartUploadHandler abortMultipartUploadHandler =
                new AbortMultipartUploadHandler(environment, mockS3Client);
        Mockito.doThrow(otherException).when(mockS3Client).abortMultipartUpload(Mockito.any());
        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);
        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
