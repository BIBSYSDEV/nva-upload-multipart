package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static no.unit.nva.amazon.s3.GatewayResponse.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.amazon.s3.GatewayResponse.BODY_KEY;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class CreateUploadHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {


    public static final String ALLOWED_ORIGIN_KEY = "ALLOWED_ORIGIN";
    public static final String AWS_REGION_KEY = "AWS_REGION";
    public static final String S3_UPLOAD_BUCKET_KEY = "S3_UPLOAD_BUCKET";

    //    public final transient String clientRegion;
    public final transient String bucketName;
    private final transient String allowedOrigin;
    private final transient AmazonS3 s3Client;

    public CreateUploadHandler() {
        this(new Environment(), createAmazonS3Client());
    }


    /**
     * Construct for lambda eventhandler to create an upload request for S3.
     */
    public CreateUploadHandler(Environment environment, AmazonS3 s3Client) {
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


        CreateUploadRequestBody requestBody = null;
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

        ObjectMetadata objectMetadata = getObjectMetadata(requestBody);

        String keyName = UUID.randomUUID().toString();
        InitiateMultipartUploadRequest initRequest =
                new InitiateMultipartUploadRequest(bucketName, keyName, objectMetadata);

        try {
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);
            System.out.println(initResponse);
            CreateUploadResponseBody responseBody = new CreateUploadResponseBody();
            responseBody.uploadId = initResponse.getUploadId();
            responseBody.key = keyName;
            response.setBody(new Gson().toJson(responseBody));
            response.setStatusCode(SC_CREATED);
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

    private ObjectMetadata getObjectMetadata(CreateUploadRequestBody requestBody) {
        ObjectMetadata objectMetadata = new ObjectMetadata();

        if (requestBody.filename != null && !requestBody.filename.isEmpty()) {
            objectMetadata.setContentDisposition("filename=\"" + requestBody.filename + "\"");
        }
        if (requestBody.mimetype != null && !requestBody.mimetype.isEmpty() && requestBody.mimetype.contains("/")) {
            objectMetadata.setContentType(requestBody.mimetype);
        }

        if (requestBody.md5hash != null && !requestBody.md5hash.isEmpty()) {
            objectMetadata.setContentMD5(requestBody.md5hash);
        }
        return objectMetadata;
    }

    /**
     * Checking incoming parameters from api-gateway.
     * @param input Map of parameters from api-gateway
     * @return POJO with parameters from api-gateway
     */
    public CreateUploadRequestBody checkParameters(Map<String, Object> input) {
        if (Objects.isNull(input)) {
            throw new ParameterMissingException("input");
        }
        String body = (String) input.get(BODY_KEY);
        CreateUploadRequestBody requestBody = new Gson().fromJson(body, CreateUploadRequestBody.class);
        if (Objects.isNull(requestBody)) {
            throw new ParameterMissingException("input");
        }
        if (Objects.isNull(requestBody.filename)) {
            throw new ParameterMissingException("filename");
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