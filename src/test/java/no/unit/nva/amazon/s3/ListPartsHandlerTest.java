package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.Type;
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
public class ListPartsHandlerTest {

    public static final int SAMPLE_PART_NUMBER = 1;
    public static final String SAMPLE_ETAG = "eTag";
    public static final int SAMPLE_SIZE = 1;
    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String SAMPLE_KEY = "key";
    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();
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

    @Test
    public void testDefaultConstructor() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY, ALLOWED_ORIGIN_KEY);
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY, S3_UPLOAD_BUCKET_KEY);
        ListPartsHandler listPartsHandler = new ListPartsHandler();
        assertNotNull(listPartsHandler);
    }


    @Test
    public void testHandleRequestMissingParameters() {
        Map<String, Object> requestInput = new HashMap<>();

        ListPartsHandler listPartsHandler = new ListPartsHandler(environment, null);
        final GatewayResponse response = listPartsHandler.handleRequest(requestInput, null);

        assertEquals(SC_BAD_REQUEST, response.getStatusCode());

    }

    @Test
    public void testHandleRequest() {

        ListPartsRequestBody requestInputBody = new ListPartsRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        PartSummary partSummary1 = new PartSummary();
        partSummary1.setPartNumber(1);
        partSummary1.setETag("ETag1");
        partSummary1.setSize(1);
        List<PartSummary> partsSummary = new ArrayList<>();
        partsSummary.add(partSummary1);

        PartSummary partSummary2 = new PartSummary();
        partSummary2.setPartNumber(2);
        partSummary2.setETag("ETag2");
        partSummary2.setSize(2);
        partsSummary.add(partSummary2);

        PartListing listPartsResponse = new PartListing();
        listPartsResponse.setParts(partsSummary);

        AmazonS3 mockS3Client = mock(AmazonS3.class);
        when(mockS3Client.listParts(Mockito.any(ListPartsRequest.class))).thenReturn(listPartsResponse);
        ListPartsHandler listPartsHandler = new ListPartsHandler(environment, mockS3Client);
        final GatewayResponse response = listPartsHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Type type = new TypeToken<List<PartSummary>>() {
        }.getType();
        final List<String> responseBody = new Gson().fromJson(response.getBody(), type);

        assertNotNull(responseBody);
    }

    @Test
    public void testHandleRequestWithManyParts() {

        ListPartsRequestBody requestInputBody = new ListPartsRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        PartSummary partSummary1 = new PartSummary();
        partSummary1.setPartNumber(1);
        partSummary1.setETag("ETag1");
        partSummary1.setSize(1);
        List<PartSummary> partsSummary = new ArrayList<>();
        partsSummary.add(partSummary1);

        PartSummary partSummary2 = new PartSummary();
        partSummary2.setPartNumber(2);
        partSummary2.setETag("ETag2");
        partSummary2.setSize(2);
        partsSummary.add(partSummary2);

        PartListing listPartsResponse = Mockito.spy(new PartListing());
        boolean isTruncated = false;

        Mockito.doReturn(true, false).when(listPartsResponse).isTruncated();

        listPartsResponse.setParts(partsSummary);

        AmazonS3 mockS3Client = mock(AmazonS3.class);
        when(mockS3Client.listParts(Mockito.any(ListPartsRequest.class))).thenReturn(listPartsResponse);

        ListPartsHandler listPartsHandler = new ListPartsHandler(environment, mockS3Client);
        final GatewayResponse response = listPartsHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Type type = new TypeToken<List<PartSummary>>() {
        }.getType();
        final List<String> responseBody = new Gson().fromJson(response.getBody(), type);

        assertNotNull(responseBody);
    }



    @Test
    public void testHandleFailingRequest() {

        ListPartsRequestBody requestInputBody = new ListPartsRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        AmazonS3Exception amazonS3Exception = new AmazonS3Exception("mock-exception");
        when(mockS3Client.listParts(Mockito.any(ListPartsRequest.class))).thenThrow(amazonS3Exception);
        ListPartsHandler listPartsHandler = new ListPartsHandler(environment, mockS3Client);
        final GatewayResponse response = listPartsHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingInputParameters() {

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        ParameterMissingException parameterMissingException = new ParameterMissingException("mock-exception");

        ListPartsHandler listPartsHandler = new ListPartsHandler(environment, mockS3Client);
        final GatewayResponse response = listPartsHandler.handleRequest(null, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingInputParametersBody() {


        AmazonS3 mockS3Client = mock(AmazonS3.class);

        Map<String, Object> requestInput = new HashMap<>();

        ListPartsHandler listPartsHandler = new ListPartsHandler(environment, mockS3Client);
        final GatewayResponse response = listPartsHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    public void testHandleFailingRequestMissingParameterUploadId() {

        ListPartsRequestBody requestInputBody = new ListPartsRequestBody(null, SAMPLE_KEY);
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        PartListing partListingResponse = new PartListing();
        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.listParts(Mockito.any(ListPartsRequest.class))).thenThrow(sdkClientException);
        ListPartsHandler listPartsHandler = new ListPartsHandler(environment, mockS3Client);
        final GatewayResponse response = listPartsHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingParameterKey() {

        ListPartsRequestBody requestInputBody = new ListPartsRequestBody(SAMPLE_UPLOAD_ID, null);

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        PartListing partListingResponse = new PartListing();
        SdkClientException sdkClientException = new SdkClientException("mock-exception");
        when(mockS3Client.listParts(Mockito.any(ListPartsRequest.class))).thenThrow(sdkClientException);
        ListPartsHandler listPartsHandler = new ListPartsHandler(environment, mockS3Client);
        final GatewayResponse response = listPartsHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    public void testHandleFailingRequestCheckParametersOtherException() {

        ListPartsRequestBody requestInputBody = new ListPartsRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);

        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);
        Exception unmappedRuntimeException = new RuntimeException("unmapped-mock-exception");

        when(mockS3Client.listParts(Mockito.any(ListPartsRequest.class))).thenThrow(unmappedRuntimeException);

        ListPartsHandler spyListPartsHandler = Mockito.spy(new ListPartsHandler(environment, mockS3Client));
        Mockito.doThrow(unmappedRuntimeException).when(spyListPartsHandler).checkParameters(Mockito.anyMap());
        final GatewayResponse response = spyListPartsHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestOtherException() {

        ListPartsRequestBody requestInputBody = new ListPartsRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);
        Map<String, Object> requestInput = new HashMap<>();
        requestInput.put(BODY_KEY, new Gson().toJson(requestInputBody));

        AmazonS3 mockS3Client = mock(AmazonS3.class);

        PartListing partListing = new PartListing();

        Exception janClientException = new RuntimeException("mock-jan-exception");
        when(mockS3Client.listParts(Mockito.any(ListPartsRequest.class)))
                .thenThrow(janClientException);
        ListPartsHandler listPartsHandler = new ListPartsHandler(environment, mockS3Client);
        final GatewayResponse response = listPartsHandler.handleRequest(requestInput, null);

        assertNotNull(response);
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }


    @Test
    public void testListPartElements() {
        PartSummary partSummary = new PartSummary();
        partSummary.setPartNumber(SAMPLE_PART_NUMBER);
        partSummary.setETag(SAMPLE_ETAG);
        partSummary.setSize(SAMPLE_SIZE);

        final ListPartsElement listPartsElement = new ListPartsElement(partSummary);


        assertEquals(SAMPLE_ETAG, listPartsElement.getEtag());
        assertEquals(Integer.toString(SAMPLE_PART_NUMBER), listPartsElement.getPartNumber());
        assertEquals(Integer.toString(SAMPLE_SIZE), listPartsElement.getSize());

        listPartsElement.setEtag(SAMPLE_ETAG);
        listPartsElement.setPartNumber(Integer.toString(SAMPLE_PART_NUMBER));
        listPartsElement.setSize(Integer.toString(SAMPLE_SIZE));

        assertEquals(SAMPLE_ETAG, listPartsElement.getEtag());
        assertEquals(Integer.toString(SAMPLE_PART_NUMBER), listPartsElement.getPartNumber());
        assertEquals(Integer.toString(SAMPLE_SIZE), listPartsElement.getSize());


    }

}
