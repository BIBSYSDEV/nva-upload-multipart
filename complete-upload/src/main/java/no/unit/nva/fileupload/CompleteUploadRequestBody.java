package no.unit.nva.fileupload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CompleteUploadRequestBody {

    private final String uploadId;
    private final String key;
    private final List<CompleteUploadPart> parts;


    /**
     * Creates an request to complete aa S3 upload.
     * @param uploadId  identifier of the upload from create
     * @param key bucket key for identify s3 object
     * @param parts list of uploaded pars index and eTag
     */
    @JsonCreator
    public CompleteUploadRequestBody(
            @JsonProperty("uploadId") String uploadId,
            @JsonProperty("key") String key,
            @JsonProperty("parts") List<CompleteUploadPart> parts) {
        this.uploadId = uploadId;
        this.key = key;
        this.parts = parts;
    }


    public String getUploadId() {
        return uploadId;
    }

    public String getKey() {
        return key;
    }

    public List<CompleteUploadPart> getParts() {
        return parts;
    }
}
