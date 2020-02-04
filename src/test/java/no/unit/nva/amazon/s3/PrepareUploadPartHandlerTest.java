package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.unit.nva.amazon.s3.GatewayResponse.BODY_KEY;
import static no.unit.nva.amazon.s3.PrepareUploadPartHandler.ALLOWED_ORIGIN_KEY;
import static no.unit.nva.amazon.s3.PrepareUploadPartHandler.S3_UPLOAD_BUCKET_KEY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"checkstyle:javadoctype", "checkstyle:MissingJavadocMethod"})
public class PrepareUploadPartHandlerTest {

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
        PrepareUploadPartRequestBody requestInputBody = new PrepareUploadPartRequestBody();
        requestInputBody.key = "key";
        requestInputBody.uploadId = "uploadId";
        requestInputBody.body = "body";
        requestInputBody.number = "1";
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
        final URL url  = new Gson().fromJson(response.getBody(), URL.class);

        assertNotNull(url);
    }

    @Test
    public void testHandleFailingRequest() {

        PrepareUploadPartRequestBody requestInputBody = createRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(sdkClientException);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler(environment, mockS3Client);
        final GatewayResponse response = prepareUploadPartHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
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

        PrepareUploadPartRequestBody requestInputBody = createRequestBody();
        requestInputBody.uploadId = null;

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

        PrepareUploadPartRequestBody requestInputBody = createRequestBody();
        requestInputBody.key = null;

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

        PrepareUploadPartRequestBody requestInputBody = createRequestBody();
        requestInputBody.number = null;

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


}
