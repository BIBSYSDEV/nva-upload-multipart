package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public class CompleteUploadHandlerTest {

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

    private CompleteUploadRequestBody createRequestBody() {
        CompleteUploadRequestBody requestInputBody = new CompleteUploadRequestBody();
        requestInputBody.key = "key";
        requestInputBody.uploadId = "uploadId";
        List<CompleteUploadPart> partEtags = new ArrayList<>();
        partEtags.add(new CompleteUploadPart("1","eTag1"));
        requestInputBody.parts = partEtags;

        return requestInputBody;
    }
    
    
    @Test
    public void testDefaultConstructor() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY, ALLOWED_ORIGIN_KEY);
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY, S3_UPLOAD_BUCKET_KEY);
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler();
        assertNotNull(completeUploadHandler);
    }


    @Test
    public void testHandleRequestMissingParameters() {
        Map<String, Object> requestInput = new HashMap<>();
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler(environment, null);
        final GatewayResponse response = completeUploadHandler.handleRequest(requestInput, null);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testHandleRequest() throws MalformedURLException {

        CompleteUploadRequestBody requestInputBody = createRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);
        CompleteMultipartUploadResult uploadResult = new CompleteMultipartUploadResult();
        when(mockS3Client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
                .thenReturn(uploadResult);
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler(environment, mockS3Client);
        final GatewayResponse response = completeUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequest() {

        CompleteUploadRequestBody requestInputBody = createRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        AmazonS3Exception amazonS3Exception = new AmazonS3Exception("mock-exception");

        when(mockS3Client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
                .thenThrow(amazonS3Exception);
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler(environment, mockS3Client);
        final GatewayResponse response = completeUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    public void testHandleFailingRequestMissingInputParameters() {

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler(environment, mockS3Client);
        final GatewayResponse response = completeUploadHandler.handleRequest(null, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingInputParametersBody() {


        AmazonS3 mockS3Client = mock(AmazonS3.class);

        Map<String, Object> requestInput = new HashMap<>();

        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler(environment, mockS3Client);
        final GatewayResponse response = completeUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    public void testHandleFailingRequestMissingParameterUploadId() {

        CompleteUploadRequestBody requestInputBody = createRequestBody();
        requestInputBody.uploadId = null;

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class)))
                .thenThrow(sdkClientException);
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler(environment, mockS3Client);
        final GatewayResponse response = completeUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingParameterKey() {

        CompleteUploadRequestBody requestInputBody = createRequestBody();
        requestInputBody.key = null;

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
                .thenThrow(sdkClientException);
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler(environment, mockS3Client);
        final GatewayResponse response = completeUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingParameterNumber() {

        CompleteUploadRequestBody requestInputBody = createRequestBody();
        requestInputBody.parts = null;

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
                .thenThrow(sdkClientException);
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler(environment, mockS3Client);
        final GatewayResponse response = completeUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }



    @Test
    public void testHandleFailingRequestCheckParametersOtherException() {

        CompleteUploadRequestBody requestInputBody = createRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);
        Exception unmappedRuntimeException = new RuntimeException("unmapped-mock-exception");

        when(mockS3Client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
                .thenThrow(unmappedRuntimeException);

        CompleteUploadHandler spyCompleteUploadHandler =
                Mockito.spy(new CompleteUploadHandler(environment, mockS3Client));
        Mockito.doThrow(unmappedRuntimeException).when(spyCompleteUploadHandler).checkParameters(Mockito.anyMap());
        final GatewayResponse response = spyCompleteUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestOtherException() {

        CompleteUploadRequestBody requestInputBody = createRequestBody();

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);


        Exception janClientException = new RuntimeException("mock-jan-exception");
        when(mockS3Client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class)))
                .thenThrow(janClientException);
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler(environment, mockS3Client);
        final GatewayResponse response = completeUploadHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }


}