package no.unit.nva.fileupload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import no.unit.nva.fileupload.model.ListPartsElement;
import no.unit.nva.fileupload.model.ListPartsRequestBody;
import no.unit.nva.fileupload.model.ListPartsResponseBody;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.testutils.HandlerUtils;
import no.unit.nva.testutils.TestContext;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static nva.commons.handlers.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"checkstyle:javadoctype", "checkstyle:MissingJavadocMethod"})
public class ListPartsHandlerTest {

    public static final int SAMPLE_PART_NUMBER = 1;
    public static final String SAMPLE_ETAG = "eTag";
    public static final int SAMPLE_SIZE = 1;
    public static final String SAMPLE_UPLOAD_ID = "uploadId";
    public static final String SAMPLE_KEY = "key";
    public static final String TEST_BUCKET_NAME = "bucketName";
    public static final String WILDCARD = "*";

    private Environment environment;
    private ListPartsHandler listPartsHandler;
    private HandlerUtils handlerUtils;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private AmazonS3Client s3client;

    /**
     * Setup test env.
     */
    @Before
    public void setUp() {
        environment = mock(nva.commons.utils.Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
        when(environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)).thenReturn(S3Constants.S3_UPLOAD_BUCKET_KEY);
        s3client = mock(AmazonS3Client.class);
        listPartsHandler = new ListPartsHandler(environment, s3client, TEST_BUCKET_NAME);
        context = new TestContext();
        handlerUtils = new HandlerUtils(objectMapper);
        outputStream = new ByteArrayOutputStream();
    }


    @Test
    public void testHandleRequestMissingParameters() throws IOException {
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(null, null);
        listPartsHandler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<ListPartsResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                nva.commons.handlers.GatewayResponse.class);

        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testHandleRequest() throws IOException {
        when(s3client.listParts(any(ListPartsRequest.class))).thenReturn(listPartsResponse());
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(listPartsRequestBody(), null);

        listPartsHandler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<ListPartsResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                nva.commons.handlers.GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
        ListPartsResponseBody responseBody = response.getBodyObject(ListPartsResponseBody.class);
        assertNotNull(responseBody);
    }

    @Test
    public void testHandleRequestWithManyParts() throws IOException {
        PartListing partListing = truncatedPartListing();
        when(s3client.listParts(any(ListPartsRequest.class))).thenReturn(partListing);

        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(listPartsRequestBody(), null);
        listPartsHandler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<ListPartsResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getBody());
        ListPartsResponseBody responseBody = response.getBodyObject(ListPartsResponseBody.class);
        assertNotNull(responseBody);
    }

    @Test
    public void testHandleFailingRequest() throws IOException {
        when(s3client.listParts(any(ListPartsRequest.class))).thenThrow(AmazonS3Exception.class);

        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(listPartsRequestBody(), null);
        listPartsHandler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<ListPartsResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testHandleFailingRequestMissingInputParameters() throws IOException {
        InputStream inputStream = handlerUtils
                .requestObjectToApiGatewayRequestInputSteam(null, null);
        listPartsHandler.handleRequest(inputStream, outputStream, context);
        GatewayResponse<ListPartsResponseBody> response = objectMapper.readValue(
                outputStream.toByteArray(),
                GatewayResponse.class);

        assertNotNull(response);
        assertEquals(SC_BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testListPartElements() {
        PartSummary partSummary = new PartSummary();
        partSummary.setPartNumber(SAMPLE_PART_NUMBER);
        partSummary.setETag(SAMPLE_ETAG);
        partSummary.setSize(SAMPLE_SIZE);

        final ListPartsElement listPartsElement = ListPartsElement.of(partSummary);


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

    protected ListPartsRequestBody listPartsRequestBody() {
        return new ListPartsRequestBody(SAMPLE_UPLOAD_ID, SAMPLE_KEY);
    }

    protected PartListing listPartsResponse() {
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
        return listPartsResponse;
    }

    protected PartListing truncatedPartListing() {
        PartListing partListing = mock(PartListing.class);
        when(partListing.getParts()).thenReturn(listPartsResponse().getParts());
        when(partListing.isTruncated()).thenReturn(true).thenReturn(false);
        return partListing;
    }

}
