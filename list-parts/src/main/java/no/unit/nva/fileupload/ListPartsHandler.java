package no.unit.nva.fileupload;

import static java.util.Objects.requireNonNull;
import static no.unit.nva.fileupload.util.S3Constants.AWS_REGION_KEY;
import static no.unit.nva.fileupload.util.S3Utils.createAmazonS3Client;
import static org.apache.http.HttpStatus.SC_OK;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartListing;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.fileupload.exception.InvalidInputException;
import no.unit.nva.fileupload.exception.NotFoundException;
import no.unit.nva.fileupload.util.S3Constants;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ListPartsHandler extends ApiGatewayHandler<ListPartsRequestBody, ListPartsResponseBody> {

    private static final Logger logger = LoggerFactory.getLogger(ListPartsHandler.class);
    public static final String S3_ERROR = "S3 error";

    private final transient String bucketName;
    private final transient AmazonS3 s3Client;

    /**
     * Default constructor for ListPartsHandler.
     */
    @JacocoGenerated
    public ListPartsHandler() {
        this(new Environment());
    }

    /**
     * Default constructor for ListPartsHandler.
     *
     * @param environment   environment reader
     */
    @JacocoGenerated
    public ListPartsHandler(Environment environment) {
        this(
                environment,
                createAmazonS3Client(environment.readEnv(AWS_REGION_KEY)),
                environment.readEnv(S3Constants.S3_UPLOAD_BUCKET_KEY)
        );
    }

    /**
     * Constructor for lambda event handler to create an upload request for S3.
     */
    public ListPartsHandler(Environment environment, AmazonS3 s3Client, String bucketName) {
        super(ListPartsRequestBody.class, environment);
        this.bucketName = bucketName;
        this.s3Client = s3Client;
    }

    @Override
    protected void validateRequest(ListPartsRequestBody listPartsRequestBody, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validate(listPartsRequestBody);
    }

    @Override
    protected ListPartsResponseBody processInput(ListPartsRequestBody input, RequestInfo requestInfo,
                                                 Context context) throws ApiGatewayException {

        ListPartsRequest listPartsRequest = toListPartsRequest(input);
        List<ListPartsElement> listParts = getListParts(listPartsRequest);
        return ListPartsResponseBody.of(listParts);
    }

    private ListPartsRequest toListPartsRequest(ListPartsRequestBody input) {
        return new ListPartsRequest(bucketName, input.getKey(), input.getUploadId());
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private List<ListPartsElement> getListParts(ListPartsRequest listPartsRequest) throws NotFoundException {
        List<ListPartsElement> listPartsElements = new ArrayList<>();

        try {
            PartListing partListing = s3Client.listParts(listPartsRequest);
            boolean moreParts = true;
            while (moreParts) {
                partListing.getParts()
                        .stream()
                        .map(ListPartsElement::of)
                        .forEach(listPartsElements::add);
                if (partListing.isTruncated()) {
                    Integer partNumberMarker = partListing.getNextPartNumberMarker();
                    listPartsRequest.setPartNumberMarker(partNumberMarker);
                    partListing = s3Client.listParts(listPartsRequest);
                } else {
                    moreParts = false;
                }
            }
        } catch (AmazonS3Exception e) {
            throw new NotFoundException(S3_ERROR, e);
        }

        return listPartsElements;
    }

    private void validate(ListPartsRequestBody input) throws InvalidInputException {
        try {
            requireNonNull(input);
            requireNonNull(input.getKey());
            requireNonNull(input.getUploadId());
        } catch (Exception e) {
            logger.warn(e.getMessage());
            throw new InvalidInputException(e);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(ListPartsRequestBody input, ListPartsResponseBody output) {
        return SC_OK;
    }
}
