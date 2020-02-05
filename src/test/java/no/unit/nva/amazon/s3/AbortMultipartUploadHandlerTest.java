package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static no.unit.nva.amazon.s3.Environment.ALLOWED_ORIGIN_KEY;
import static no.unit.nva.amazon.s3.Environment.S3_UPLOAD_BUCKET_KEY;
import static no.unit.nva.amazon.s3.GatewayResponse.BODY_KEY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
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
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Test
    public void testDefaultConstructor() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY,ALLOWED_ORIGIN_KEY);
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY,S3_UPLOAD_BUCKET_KEY);
        AbortMultipartUploadHandler abortMultipartUploadHandler = new AbortMultipartUploadHandler();
        assertNotNull(abortMultipartUploadHandler);
    }


    @Test
    public void testHandleRequestMissingParameters() {
        Map<String, Object> requestInput = new HashMap<>();

        AbortMultipartUploadHandler abortMultipartUploadHandler =
                new AbortMultipartUploadHandler(environment, null);
        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);

        assertEquals(SC_BAD_REQUEST, response.getStatusCode());

    }

    @Test
    public void testHandleRequest() {

        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        AbortMultipartUploadHandler createUploadHandler = new AbortMultipartUploadHandler(environment, mockS3Client);
        final GatewayResponse response = createUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    private AbortMultipartUploadRequestBody createAbortMultipartUploadRequestBody() {
        AbortMultipartUploadRequestBody requestInputBody = new AbortMultipartUploadRequestBody();
        requestInputBody.uploadId = "uploadId";
        requestInputBody.key = "key";
        return requestInputBody;
    }

    @Test
    public void testHandleFailingRequest() {

        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client =  mock(AmazonS3.class);

        SdkClientException sdkClientException = new SdkClientException("mock-exception");

        AbortMultipartUploadHandler abortMultipartUploadHandler =
                Mockito.spy(new AbortMultipartUploadHandler(environment, mockS3Client));
        doThrow(sdkClientException).when(mockS3Client).abortMultipartUpload(Mockito.any());

        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestNoInput() {


        AmazonS3 mockS3Client =  mock(AmazonS3.class);

        ParameterMissingException  parameterMissingException = new ParameterMissingException("mock-exception");

        AbortMultipartUploadHandler abortMultipartUploadHandler =
                Mockito.spy(new AbortMultipartUploadHandler(environment, mockS3Client));
        doThrow(parameterMissingException).when(abortMultipartUploadHandler)
                .handleRequest(Mockito.anyMap(), Mockito.any());

        AbortMultipartUploadHandler createUploadHandler = new AbortMultipartUploadHandler(environment, mockS3Client);
        final GatewayResponse response = createUploadHandler.handleRequest(null, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingParameterUploadId() {

        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();
        requestInputBody.uploadId = null;

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
    public void testHandleFailingRequestMissingParameterKey() {

        AbortMultipartUploadRequestBody requestInputBody = createAbortMultipartUploadRequestBody();
        requestInputBody.key = null;

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
        AbortMultipartUploadHandler spyUploadHandler =
                Mockito.spy(new AbortMultipartUploadHandler(environment, mockS3Client));
        Mockito.doThrow(unmappedRuntimeException).when(spyUploadHandler).checkParameters(Mockito.anyMap());
        final GatewayResponse response = spyUploadHandler.handleRequest(requestInput, null);

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
                Mockito.spy(new AbortMultipartUploadHandler(environment, mockS3Client));
        Mockito.doThrow(otherException).when(mockS3Client).abortMultipartUpload(Mockito.any());

        final GatewayResponse response = abortMultipartUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }


}
