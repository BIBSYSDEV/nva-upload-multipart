package no.unit.nva.amazon.s3.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AbortMultipartUploadRequestBody {

    private String uploadId;
    private String key;

    @JsonCreator
    public AbortMultipartUploadRequestBody(
            @JsonProperty("uploadId") String uploadId,
            @JsonProperty("key") String key) {
        this.uploadId = uploadId;
        this.key = key;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
