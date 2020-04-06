package no.unit.nva.amazon.s3;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static no.unit.nva.amazon.s3.Environment.ALLOWED_ORIGIN_KEY;
import static no.unit.nva.amazon.s3.Environment.MISSING_ENV_TEXT;
import static no.unit.nva.amazon.s3.Environment.S3_UPLOAD_BUCKET_KEY;
import static no.unit.nva.amazon.s3.GatewayResponse.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.amazon.s3.GatewayResponse.BODY_KEY;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class AbortMultipartUploadHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {

    public static final String MULTIPART_UPLOAD_ABORTED_MESSAGE = "Multipart Upload aborted";
    public static final String PARAMETER_INPUT_KEY = "input";
    public static final String PARAMETER_BODY_KEY = "body";
    public static final String PARAMETER_UPLOAD_ID_KEY = "uploadId";
    public static final String PARAMETER_KEY_KEY = "key";

    public final transient String bucketName;
    private final transient String allowedOrigin;
    private static final AmazonS3 s3Client = createAmazonS3Client();

    public AbortMultipartUploadHandler() {
        this(new Environment());
    }

    /**
     * Construct for lambda eventhandler to create an upload request for S3.
     */
    public AbortMultipartUploadHandler(Environment environment) {
        this.allowedOrigin = environment.get(ALLOWED_ORIGIN_KEY)
                .orElseThrow(() -> new  IllegalStateException(String.format(MISSING_ENV_TEXT,ALLOWED_ORIGIN_KEY)));
        this.bucketName = environment.get(S3_UPLOAD_BUCKET_KEY)
                .orElseThrow(() -> new  IllegalStateException(String.format(MISSING_ENV_TEXT,S3_UPLOAD_BUCKET_KEY)));
    }

    private static AmazonS3 createAmazonS3Client() {
        return AmazonS3ClientBuilder.standard().build();
    }

    public  AmazonS3 getS3Client() {
        return s3Client;
    }

    @Override
    public GatewayResponse handleRequest(Map<String, Object> input, Context context) {

        final GatewayResponse response = new GatewayResponse();
        response.setHeaders(headers());


        AbortMultipartUploadRequestBody requestBody = null;
        try {
            requestBody = checkParameters(input);
        } catch (JsonSyntaxException | ParameterMissingException e) {
            System.out.println(DebugUtils.dumpException(e));
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_BAD_REQUEST);
            System.out.println(response);
            return response;
        } catch (Exception e) {
            System.out.println(DebugUtils.dumpException(e));
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_INTERNAL_SERVER_ERROR);
            System.out.println(response);
            return response;
        }

        try {
            AbortMultipartUploadRequest abortMultipartUploadRequest =
                    new AbortMultipartUploadRequest(bucketName, requestBody.getKey(), requestBody.getUploadId());
            getS3Client().abortMultipartUpload(abortMultipartUploadRequest);
            SimpleMessageResponse  message = new SimpleMessageResponse(MULTIPART_UPLOAD_ABORTED_MESSAGE);
            response.setBody(new Gson().toJson(message));
            response.setStatusCode(SC_OK);
            System.out.println(response);
        } catch (AmazonS3Exception e) {
            System.out.println(DebugUtils.dumpException(e));
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_NOT_FOUND);
        } catch (Exception e) {
            System.out.println(DebugUtils.dumpException(e));
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_INTERNAL_SERVER_ERROR);
            System.out.println(response);
            return response;
        }

        return response;
    }


    /**
     * Checks parameters for abortint multipart upload.
     *
     * @param input Map with parameters from API-gateway
     * @return POJO with extracted parameters
     */
    public AbortMultipartUploadRequestBody checkParameters(Map<String, Object> input) {
        if (Objects.isNull(input)) {
            throw new ParameterMissingException(PARAMETER_INPUT_KEY);
        }
        String body = (String) input.get(BODY_KEY);
        AbortMultipartUploadRequestBody requestBody = new Gson().fromJson(body, AbortMultipartUploadRequestBody.class);
        if (Objects.isNull(requestBody)) {
            throw new ParameterMissingException(PARAMETER_BODY_KEY);
        }
        if (Objects.isNull(requestBody.getUploadId())) {
            throw new ParameterMissingException(PARAMETER_UPLOAD_ID_KEY);
        }
        if (Objects.isNull(requestBody.getKey())) {
            throw new ParameterMissingException(PARAMETER_KEY_KEY);
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

