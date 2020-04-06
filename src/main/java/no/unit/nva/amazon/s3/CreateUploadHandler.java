package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
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

import static no.unit.nva.amazon.s3.Environment.ALLOWED_ORIGIN_KEY;
import static no.unit.nva.amazon.s3.Environment.MISSING_ENV_TEXT;
import static no.unit.nva.amazon.s3.Environment.S3_UPLOAD_BUCKET_KEY;
import static no.unit.nva.amazon.s3.GatewayResponse.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class CreateUploadHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {


    public static final String PARAMETER_BODY_KEY = "body";
    public static final String PARAMETER_FILENAME_KEY = "filename";
    public static final String PARAMETER_INPUT_KEY = "input";

    private final transient AmazonS3 s3Client;
    public final transient String bucketName;
    private final transient String allowedOrigin;


    public CreateUploadHandler() {
        this(new Environment(), createAmazonS3Client());
    }


    /**
     * Construct for lambda eventhandler to create an upload request for S3.
     */
    public CreateUploadHandler(Environment environment, AmazonS3 s3Client) {
        this.allowedOrigin = environment.get(ALLOWED_ORIGIN_KEY)
                .orElseThrow(() -> new  IllegalStateException(String.format(MISSING_ENV_TEXT,ALLOWED_ORIGIN_KEY)));
        this.bucketName = environment.get(S3_UPLOAD_BUCKET_KEY)
                .orElseThrow(() -> new  IllegalStateException(String.format(MISSING_ENV_TEXT,S3_UPLOAD_BUCKET_KEY)));
        this.s3Client = s3Client;

    }

    private static AmazonS3 createAmazonS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.EU_WEST_1)
                .build();
    }

    public AmazonS3 getS3Client() {
        return s3Client;
    }

    @Override
    public GatewayResponse handleRequest(Map<String, Object> input, Context context) {

        final GatewayResponse response = new GatewayResponse();
        response.setHeaders(headers());


        CreateUploadRequestBody requestBody = null;
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
            ObjectMetadata objectMetadata = getObjectMetadata(requestBody);

            String keyName = UUID.randomUUID().toString();
            InitiateMultipartUploadRequest initRequest =
                    new InitiateMultipartUploadRequest(bucketName, keyName, objectMetadata);
            InitiateMultipartUploadResult initResponse = getS3Client().initiateMultipartUpload(initRequest);
            System.out.println(initResponse);
            CreateUploadResponseBody responseBody = new CreateUploadResponseBody(initResponse.getUploadId(), keyName);
            response.setBody(new Gson().toJson(responseBody));
            response.setStatusCode(SC_CREATED);
        } catch (SdkClientException e) {
            System.out.println(DebugUtils.dumpException(e));
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.out.println(DebugUtils.dumpException(e));
            response.setErrorBody(e.getMessage());
            response.setStatusCode(SC_INTERNAL_SERVER_ERROR);
            return response;
        }
        return response;
    }

    /**
     * Extracting metadata from the given file resource.
     * @param requestBody incoming parameters
     * @return Metadata to be stored with file on S3
     */
    public ObjectMetadata getObjectMetadata(CreateUploadRequestBody requestBody) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentMD5(null);

        if (!Objects.isNull(requestBody.getFilename()) && !requestBody.getFilename().isEmpty()) {
            objectMetadata.setContentDisposition("filename=\"" + requestBody.getFilename() + "\"");
        }
        if (!(Objects.isNull(requestBody.getMimetype()) || requestBody.getMimetype().isEmpty())
                && requestBody.getMimetype().contains("/")) {
            objectMetadata.setContentType(requestBody.getMimetype());
        }

        return objectMetadata;
    }

    /**
     * Checking incoming parameters from api-gateway.
     *
     * @param input Map of parameters from api-gateway
     * @return POJO with parameters from api-gateway
     */
    public CreateUploadRequestBody checkParameters(Map<String, Object> input) {
        if (Objects.isNull(input)) {
            throw new ParameterMissingException(PARAMETER_INPUT_KEY);
        }
        String body = (String) input.get(PARAMETER_BODY_KEY);
        CreateUploadRequestBody requestBody = new Gson().fromJson(body, CreateUploadRequestBody.class);
        if (Objects.isNull(requestBody)) {
            throw new ParameterMissingException(PARAMETER_BODY_KEY);
        }
        if (Objects.isNull(requestBody.getFilename())) {
            throw new ParameterMissingException(PARAMETER_FILENAME_KEY);
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