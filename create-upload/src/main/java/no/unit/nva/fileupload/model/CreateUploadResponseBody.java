package no.unit.nva.fileupload.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.utils.JacocoGenerated;

import java.util.Objects;

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

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreateUploadResponseBody that = (CreateUploadResponseBody) o;
        return Objects.equals(getUploadId(), that.getUploadId())
                && Objects.equals(getKey(), that.getKey());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getUploadId(), getKey());
    }

    public String getUploadId() {
        return uploadId;
    }

    public String getKey() {
        return key;
    }
}
