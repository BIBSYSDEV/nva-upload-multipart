package no.unit.nva.amazon.s3;


import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static no.unit.nva.amazon.s3.Environment.ALLOWED_ORIGIN_KEY;
import static no.unit.nva.amazon.s3.Environment.S3_UPLOAD_BUCKET_KEY;
import static no.unit.nva.amazon.s3.GatewayResponse.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.amazon.s3.GatewayResponse.BODY_KEY;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;


public class PrepareUploadPartHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {


    public static final String PARAMETER_UPLOAD_ID_KEY = "uploadId";
    public static final String PARAMETER_PART_NUMBER_KEY = "partNumber";
    public static final String PARAMETER_INPUT_KEY = "input";
    public static final String PARAMETER_BODY_KEY = "body";
    public static final String PARAMETER_KEY_KEY = "key";
    public static final String PARAMETER_NUMBER_KEY = "number";

    public final transient String bucketName;
    private final transient String allowedOrigin;
    private final transient AmazonS3 s3Client;

    public PrepareUploadPartHandler() {
        this(new Environment(), createAmazonS3Client());
    }


    /**
     * Construct for lambda eventhandler to create an upload request for S3.
     */
    public PrepareUploadPartHandler(Environment environment, AmazonS3 s3Client) {
        this.allowedOrigin = environment.get(ALLOWED_ORIGIN_KEY).orElseThrow(IllegalStateException::new);
        this.bucketName = environment.get(S3_UPLOAD_BUCKET_KEY).orElseThrow(IllegalStateException::new);
        this.s3Client = s3Client;
    }

    public static AmazonS3 createAmazonS3Client() {
        return AmazonS3ClientBuilder.standard()
                .build();
    }

    @Override
    public GatewayResponse handleRequest(Map<String, Object> input, Context context) {

        final GatewayResponse response = new GatewayResponse();
        response.setHeaders(headers());


        PrepareUploadPartRequestBody requestBody = null;
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


            GeneratePresignedUrlRequest predesignedUrlUploadRequest =
                    new GeneratePresignedUrlRequest(bucketName, requestBody.key)
                            .withMethod(HttpMethod.PUT);
            predesignedUrlUploadRequest.addRequestParameter(PARAMETER_UPLOAD_ID_KEY, requestBody.uploadId);
            predesignedUrlUploadRequest.addRequestParameter(PARAMETER_PART_NUMBER_KEY, requestBody.number);

            URL predesignedUloadUrl = s3Client.generatePresignedUrl(predesignedUrlUploadRequest);


            System.out.println(predesignedUloadUrl);
            response.setBody(new Gson().toJson(predesignedUloadUrl));
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
     *
     * @param input MAp of parameters from api-gateway
     * @return POJO with checked parameters
     */
    public PrepareUploadPartRequestBody checkParameters(Map<String, Object> input) {
        if (Objects.isNull(input)) {
            throw new ParameterMissingException(PARAMETER_INPUT_KEY);
        }
        String body = (String) input.get(BODY_KEY);
        PrepareUploadPartRequestBody requestBody = new Gson().fromJson(body, PrepareUploadPartRequestBody.class);
        if (Objects.isNull(requestBody)) {
            throw new ParameterMissingException(PARAMETER_BODY_KEY);
        }
        if (Objects.isNull(requestBody.uploadId)) {
            throw new ParameterMissingException(PARAMETER_UPLOAD_ID_KEY);
        }
        if (Objects.isNull(requestBody.key)) {
            throw new ParameterMissingException(PARAMETER_KEY_KEY);
        }
        if (Objects.isNull(requestBody.number)) {
            throw new ParameterMissingException(PARAMETER_NUMBER_KEY);
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
