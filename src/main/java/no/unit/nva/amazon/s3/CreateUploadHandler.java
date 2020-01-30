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

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_CREATED;


public class CreateUploadHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {

    public static final String allowedOrigin = "*";
    public static final Regions clientRegion = Regions.EU_WEST_1;
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String BODY = "body";


    @Override
    public GatewayResponse handleRequest(Map<String, Object> input, Context context) {

        try {

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
            System.out.println(initResponse);
            CreateUploadResponseBody responseBody = new CreateUploadResponseBody();
            responseBody.uploadId = initResponse.getUploadId();
            responseBody.key = keyName;

            final GatewayResponse response = new GatewayResponse(new Gson().toJson(responseBody), SC_CREATED);
            System.out.println(response);
            return response;
        } catch (Exception e) {
            System.out.println(e);
            return new GatewayResponse(null, SC_INTERNAL_SERVER_ERROR);
        }

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
}