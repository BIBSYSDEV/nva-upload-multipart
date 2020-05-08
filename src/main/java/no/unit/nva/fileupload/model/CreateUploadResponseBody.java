package no.unit.nva.fileupload.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateUploadResponseBody {

    private final String uploadId;
    private final String key;

    @JsonCreator
    public CreateUploadResponseBody(
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
