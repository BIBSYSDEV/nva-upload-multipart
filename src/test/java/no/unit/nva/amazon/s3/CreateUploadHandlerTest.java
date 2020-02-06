package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
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
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"checkstyle:javadoctype", "checkstyle:MissingJavadocMethod"})
public class CreateUploadHandlerTest {

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
        CreateUploadHandler createUploadHandler = new CreateUploadHandler();
        assertNotNull(createUploadHandler);
    }


    @Test
    public void testHandleRequestMissingParameters() {
        Map<String, Object> requestInput = new HashMap<>();

        CreateUploadHandler createUploadHandler = new CreateUploadHandler(environment);
        final GatewayResponse response = createUploadHandler.handleRequest(requestInput, null);

        assertEquals(SC_BAD_REQUEST, response.getStatusCode());

    }

    @Test
    public void testHandleRequest() {

        CreateUploadRequestBody requestInputBody = createCreateUploadRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        InitiateMultipartUploadResult createUploadResponse =  new InitiateMultipartUploadResult();
        createUploadResponse.setKey("uploadKey");
        createUploadResponse.setUploadId("uploadId");
        when(mockS3Client.initiateMultipartUpload(Mockito.any(InitiateMultipartUploadRequest.class)))
                .thenReturn(createUploadResponse);
        CreateUploadHandler createUploadHandler = Mockito.spy(new CreateUploadHandler(environment));
        Mockito.doReturn(mockS3Client).when(createUploadHandler).getS3Client();
        final GatewayResponse response = createUploadHandler.handleRequest(requestInput, null);


        assertNotNull(response);
        assertEquals(SC_CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        final CreateUploadResponseBody responseBody = new Gson().fromJson(response.getBody(),
                CreateUploadResponseBody.class);

        assertNotNull(responseBody.key);
        assertNotNull(responseBody.uploadId);
    }

    @Test
    public void testHandleFailingRequest() {

        CreateUploadRequestBody requestInputBody = createCreateUploadRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        InitiateMultipartUploadResult createUploadResponse =  new InitiateMultipartUploadResult();
        createUploadResponse.setKey("uploadKey");
        createUploadResponse.setUploadId("uploadId");
        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.initiateMultipartUpload(Mockito.any(InitiateMultipartUploadRequest.class)))
                .thenThrow(sdkClientException);
        CreateUploadHandler createUploadHandler = Mockito.spy(new CreateUploadHandler(environment));
        Mockito.doReturn(mockS3Client).when(createUploadHandler).getS3Client();
        final GatewayResponse response = createUploadHandler.handleRequest(requestInput, null);


        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestException() {

        CreateUploadRequestBody requestInputBody = createCreateUploadRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        InitiateMultipartUploadResult createUploadResponse =  new InitiateMultipartUploadResult();
        createUploadResponse.setKey("uploadKey");
        createUploadResponse.setUploadId("uploadId");
        RuntimeException runtimeException = new RuntimeException("mock-exception");
        when(mockS3Client.initiateMultipartUpload(Mockito.any(InitiateMultipartUploadRequest.class)))
                .thenThrow(runtimeException);
        CreateUploadHandler createUploadHandler = Mockito.spy(new CreateUploadHandler(environment));
        Mockito.doReturn(mockS3Client).when(createUploadHandler).getS3Client();
        final GatewayResponse response = createUploadHandler.handleRequest(requestInput, null);


        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }



    @Test
    public void testHandleFailingRequestNoInput() {


        AmazonS3 mockS3Client =  mock(AmazonS3.class);

        ParameterMissingException  parameterMissingException = new ParameterMissingException("mock-exception");
        when(mockS3Client.initiateMultipartUpload(Mockito.any(InitiateMultipartUploadRequest.class)))
                .thenThrow(parameterMissingException);
        CreateUploadHandler createUploadHandler = Mockito.spy(new CreateUploadHandler(environment));
        Mockito.doReturn(mockS3Client).when(createUploadHandler).getS3Client();
        final GatewayResponse response = createUploadHandler.handleRequest(null, null);


        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingFileparameters() {

        CreateUploadRequestBody requestInputBody = createCreateUploadRequestBody();


        requestInputBody.filename  = null;

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        InitiateMultipartUploadResult createUploadResponse =  new InitiateMultipartUploadResult();
        createUploadResponse.setKey("uploadKey");
        createUploadResponse.setUploadId("uploadId");
        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.initiateMultipartUpload(Mockito.any(InitiateMultipartUploadRequest.class)))
                .thenThrow(sdkClientException);
        CreateUploadHandler createUploadHandler = Mockito.spy(new CreateUploadHandler(environment));
        Mockito.doReturn(mockS3Client).when(createUploadHandler).getS3Client();
        final GatewayResponse response = createUploadHandler.handleRequest(requestInput, null);


        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestCheckParametersOtherException() {

        CreateUploadRequestBody requestInputBody = createCreateUploadRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        Exception unmappedRuntimeException  = new RuntimeException("unmapped-mock-exception");
        when(mockS3Client.initiateMultipartUpload(Mockito.any(InitiateMultipartUploadRequest.class)))
                .thenThrow(unmappedRuntimeException);
        CreateUploadHandler spyUploadHandler = Mockito.spy(new CreateUploadHandler(environment));
        Mockito.doThrow(unmappedRuntimeException).when(spyUploadHandler).checkParameters(Mockito.anyMap());
        final GatewayResponse response = spyUploadHandler.handleRequest(requestInput, null);


        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    public void testHandleFailingRequestOtherException() {

        CreateUploadRequestBody requestInputBody = createCreateUploadRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client =  mock(AmazonS3.class);
        InitiateMultipartUploadResult createUploadResponse =  new InitiateMultipartUploadResult();
        createUploadResponse.setKey("uploadKey");
        createUploadResponse.setUploadId("uploadId");
        Exception janClientException = new RuntimeException("mock-jan-exception");
        when(mockS3Client.initiateMultipartUpload(Mockito.any(InitiateMultipartUploadRequest.class)))
                .thenThrow(janClientException);
        CreateUploadHandler createUploadHandler = new CreateUploadHandler(environment);
        final GatewayResponse response = createUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    private CreateUploadRequestBody createCreateUploadRequestBody() {
        CreateUploadRequestBody requestInputBody = new CreateUploadRequestBody();

        requestInputBody.filename = "filename";
        requestInputBody.mimetype = "mime/type";
        requestInputBody.size = "size";
        requestInputBody.md5hash = "md5hash";
        return requestInputBody;
    }


}