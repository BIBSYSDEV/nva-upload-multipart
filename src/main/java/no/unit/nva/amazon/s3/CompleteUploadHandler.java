package no.unit.nva.amazon.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
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

public class CompleteUploadHandler implements RequestHandler<Map<String, Object>, GatewayResponse> {


    public final transient String bucketName;
    private final transient String allowedOrigin;
    private final transient AmazonS3 s3Client;

    public CompleteUploadHandler() {
        this(new Environment(), createAmazonS3Client());
    }


    /**
     * Construct for lambda eventhandler to create an upload request for S3.
     */
    public CompleteUploadHandler(Environment environment, AmazonS3 s3Client) {
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


        CompleteUploadRequestBody requestBody;
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
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest();
            completeMultipartUploadRequest.setBucketName(bucketName);
            completeMultipartUploadRequest.setKey(requestBody.key);
            completeMultipartUploadRequest.setUploadId(requestBody.uploadId);
            List<PartETag> partETags = new ArrayList<>();
            for (CompleteUploadPart part : requestBody.parts) {
                PartETag partETag = new PartETag(Integer.parseInt(part.getPartNumber()), part.getEtag());
                partETags.add(partETag);
            }
            completeMultipartUploadRequest.setPartETags(partETags);
            CompleteMultipartUploadResult uploadResult =
                    s3Client.completeMultipartUpload(completeMultipartUploadRequest);
            System.out.println(uploadResult);
            response.setBody("Upload Complete");
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
     * Checking inputparameters from api-gateway.
     *
     * @param input Map with parameters from api-gateway
     * @return POJO with requestparameters
     */
    public CompleteUploadRequestBody checkParameters(Map<String, Object> input) {
        if (Objects.isNull(input)) {
            throw new ParameterMissingException("input");
        }
        String body = (String) input.get(BODY_KEY);
        System.out.println("incoming request: " + body);
        CompleteUploadRequestBody requestBody = new Gson().fromJson(body, CompleteUploadRequestBody.class);
        if (Objects.isNull(requestBody)) {
            throw new ParameterMissingException("input");
        }
        if (Objects.isNull(requestBody.key)) {
            throw new ParameterMissingException("key");
        }
        if (Objects.isNull(requestBody.uploadId)) {
            throw new ParameterMissingException("uploadId");
        }
        if (Objects.isNull(requestBody.parts)) {
            throw new ParameterMissingException("partETags");
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