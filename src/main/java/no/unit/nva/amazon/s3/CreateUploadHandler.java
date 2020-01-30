package no.unit.nva.amazon.s3;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.gson.Gson;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;


public class CreateUploadHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {

    public static final String allowedOrigin = "*";
    public static final Regions clientRegion = Regions.EU_WEST_1;
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String BODY = "body";


    @Override
    public GatewayResponse handleRequest(Map<String, Object> input, Context context) {


        CreateUploadRequestBody requestBody = checkParameters(input);


        ObjectMetadata objectMetadata = new ObjectMetadata();

        if (requestBody.filename != null && !requestBody.filename.isEmpty()) {
            objectMetadata.setContentDisposition("filename=\"" + requestBody.filename + "\"");
        }
        if (requestBody.mimetype != null && !requestBody.mimetype.isEmpty() && requestBody.mimetype.contains("/")) {
            objectMetadata.setContentType(requestBody.mimetype);
        }

        String bucketName = new Environment().get("S3_UPLOAD_BUCKET").orElseThrow(IllegalStateException::new);

        String keyName = UUID.randomUUID().toString();
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName,
                objectMetadata);
        InitiateMultipartUploadResult initResponse = getAmazonS3Client().initiateMultipartUpload(initRequest);

        CreateUploadResponseBody responseBody = new CreateUploadResponseBody();
        responseBody.uploadId = initResponse.getUploadId();
        responseBody.key = keyName;

        return new GatewayResponse<>(responseBody, headers(), SC_OK);

    }


    private AmazonS3 getAmazonS3Client() {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(clientRegion)
                .build();
        return s3Client;
    }

    private CreateUploadRequestBody checkParameters(Map<String, Object> input) {
        String body = (String) input.get(BODY);
        CreateUploadRequestBody requestBody = new Gson().fromJson(body, CreateUploadRequestBody.class);
        return requestBody;
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
        return headers;
    }
}