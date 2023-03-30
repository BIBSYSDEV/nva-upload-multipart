package no.unit.nva.fileupload;

import static org.apache.http.HttpStatus.SC_OK;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import no.unit.nva.fileupload.util.S3Constants;
import no.unit.nva.fileupload.util.S3Utils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchSignPartHandler extends ApiGatewayHandler<Void, SignedPartResponseBody> {

    private final Environment environment;
    private final AmazonS3 s3Client;
    private final String bucketName;



    @JacocoGenerated
    public FetchSignPartHandler() {
        this(new Environment());
    }

    @JacocoGenerated
    public FetchSignPartHandler(Environment environment) {
        this(
            environment,
            S3Utils.createAmazonS3Client(environment.readEnv(S3Constants.AWS_REGION_KEY)),
            environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)
        );
    }

    public FetchSignPartHandler(Environment environment, AmazonS3 s3Client, String bucketName) {
        super(Void.class);
        this.environment = environment;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    protected SignedPartResponseBody processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return new SignedPartResponseBody(null, null);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, SignedPartResponseBody output) {
        return SC_OK;
    }
}
