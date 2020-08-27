package no.unit.nva.fileupload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AbortMultipartUploadRequestBody {
    @JsonProperty("uploadId")
    private final String uploadId;
    @JsonProperty("key")
    private final String key;

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

    public String getKey() {
        return key;
    }
}
