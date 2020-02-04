package no.unit.nva.amazon.s3;


import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static no.unit.nva.amazon.s3.GatewayResponse.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.amazon.s3.GatewayResponse.BODY_KEY;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;


public class ListPartsHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {

    public static final String ALLOWED_ORIGIN_KEY = "ALLOWED_ORIGIN";
    public static final String S3_UPLOAD_BUCKET_KEY = "S3_UPLOAD_BUCKET";

    public final transient String bucketName;
    private final transient String allowedOrigin;
    private final transient AmazonS3 s3Client;

    public ListPartsHandler() {
        this(new Environment(), createAmazonS3Client());
    }


    /**
     * Construct for lambda eventhandler to create an upload request for S3.
     */
    public ListPartsHandler(Environment environment, AmazonS3 s3Client) {
        this.allowedOrigin = environment.get(ALLOWED_ORIGIN_KEY).orElseThrow(IllegalStateException::new);
        this.bucketName = environment.get(S3_UPLOAD_BUCKET_KEY).orElseThrow(IllegalStateException::new);
        this.s3Client = s3Client;
    }

    public static AmazonS3 createAmazonS3Client() {
        return AmazonS3ClientBuilder.standard()
                .build();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    @Override
    public GatewayResponse handleRequest(Map<String, Object> input, Context context) {

        final GatewayResponse response = new GatewayResponse();
        response.setHeaders(headers());


        ListPartsRequestBody requestBody = null;
        try {
            requestBody = checkParameters(input);
        } catch (JsonSyntaxException | ParameterMissingException e) {
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_BAD_REQUEST);
            System.out.println(response);
            return response;
        } catch (Exception e) {
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_INTERNAL_SERVER_ERROR);
            System.out.println(response);
            return response;
        }

        try {

            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, requestBody.key, requestBody.uploadId);

            PartListing partListing = s3Client.listParts(listPartsRequest);
            List<ListPartsElement> listPartsElements = new ArrayList<>();
            List<PartSummary> partSummaries = partListing.getParts();

            for (PartSummary partSummary : partSummaries) {
                listPartsElements.add(new ListPartsElement(partSummary));
            }

            System.out.println(partListing);
            response.setBody(new Gson().toJson(listPartsElements));
            response.setStatusCode(SC_OK);
            System.out.println(response);
        } catch (SdkClientException e) {
            System.out.println(e);
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_INTERNAL_SERVER_ERROR);
            System.out.println(response);
            return response;
        }
        return response;
    }

    /**
     * Checks incoming parameters from api-gateway.
     * @param input MAp of parameters from api-gateway
     * @return POJO with checked parameters
     */
    public ListPartsRequestBody checkParameters(Map<String, Object> input) {
        if (Objects.isNull(input)) {
            throw new ParameterMissingException("input");
        }
        String body = (String) input.get(BODY_KEY);
        ListPartsRequestBody requestBody = new Gson().fromJson(body, ListPartsRequestBody.class);
        if (Objects.isNull(requestBody)) {
            throw new ParameterMissingException("input");
        }
        if (Objects.isNull(requestBody.uploadId)) {
            throw new ParameterMissingException("uploadId");
        }
        if (Objects.isNull(requestBody.key)) {
            throw new ParameterMissingException("key");
        }
        return requestBody;
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
        return headers;
    }

}
