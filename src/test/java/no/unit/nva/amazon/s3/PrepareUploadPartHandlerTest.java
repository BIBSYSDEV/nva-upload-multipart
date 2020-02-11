package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static no.unit.nva.amazon.s3.Environment.ALLOWED_ORIGIN_KEY;
import static no.unit.nva.amazon.s3.Environment.S3_UPLOAD_BUCKET_KEY;
import static no.unit.nva.amazon.s3.GatewayResponse.BODY_KEY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
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
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
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

    private PrepareUploadPartRequestBody createRequestBody() {
        PrepareUploadPartRequestBody requestInputBody =
                new PrepareUploadPartRequestBody(SAMPLE_UPLOADID, SAMPLE_KEY, SAMPLE_BODY, SAMPLE_PART_NUMBER);
        return requestInputBody;
    }
    
    
    @Test
    public void testDefaultConstructor() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY, ALLOWED_ORIGIN_KEY);
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY, S3_UPLOAD_BUCKET_KEY);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler();
        assertNotNull(prepareUploadPartHandler);
    }


    @Test
    public void testHandleRequestMissingParameters() {
        Map<String, Object> requestInput = new HashMap<>();
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, null);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(requestInput, null);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testHandleRequest() throws MalformedURLException {

        PrepareUploadPartRequestBody requestInputBody = createRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);
        URL dummyUrl = new URL("http://localhost");
        when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class))).thenReturn(dummyUrl);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, mockS3Client);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final PrepareUploadPartResponseBody responseBody =
                new Gson().fromJson(response.getBody(), PrepareUploadPartResponseBody.class);
        assertNotNull(responseBody.getUrl());

    }

    @Test
    public void testHandleFailingRequest() {

        PrepareUploadPartRequestBody requestInputBody = createRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        AmazonS3Exception amazonS3Exception = new AmazonS3Exception("mock-exception");
        when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(amazonS3Exception);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, mockS3Client);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    public void testHandleFailingRequestMissingInputParameters() {

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, mockS3Client);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(null, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingInputParametersBody() {


        AmazonS3 mockS3Client = mock(AmazonS3.class);

        Map<String, Object> requestInput = new HashMap<>();

        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, mockS3Client);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    public void testHandleFailingRequestMissingParameterUploadId() {

        PrepareUploadPartRequestBody requestInputBody
                = new PrepareUploadPartRequestBody(null, SAMPLE_KEY, SAMPLE_BODY, SAMPLE_PART_NUMBER);


        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(sdkClientException);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, mockS3Client);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingParameterKey() {

        PrepareUploadPartRequestBody requestInputBody =
                new PrepareUploadPartRequestBody(SAMPLE_UPLOADID, null, SAMPLE_BODY, SAMPLE_PART_NUMBER);


        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(sdkClientException);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, mockS3Client);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingParameterNumber() {

        PrepareUploadPartRequestBody requestInputBody =
                new PrepareUploadPartRequestBody(SAMPLE_UPLOADID, SAMPLE_KEY, SAMPLE_BODY, null);


        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(sdkClientException);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, mockS3Client);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }



    @Test
    public void testHandleFailingRequestCheckParametersOtherException() {

        PrepareUploadPartRequestBody requestInputBody = createRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);
        Exception unmappedRuntimeException = new RuntimeException("unmapped-mock-exception");

        when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(unmappedRuntimeException);

        PrepareUploadPartHandler spyPrepareUploadPartHandler =
                Mockito.spy(new PrepareUploadPartHandler(environment, mockS3Client));
        Mockito.doThrow(unmappedRuntimeException).when(spyPrepareUploadPartHandler).checkParameters(Mockito.anyMap());
        final GatewayResponse response = spyPrepareUploadPartHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestOtherException() {

        PrepareUploadPartRequestBody requestInputBody = createRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);


        Exception janClientException = new RuntimeException("mock-jan-exception");
        when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(janClientException);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, mockS3Client);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
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
