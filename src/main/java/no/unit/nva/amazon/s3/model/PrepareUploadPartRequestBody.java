package no.unit.nva.amazon.s3.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrepareUploadPartRequestBody {

    private final  String uploadId;
    private final  String key;
    private final  String body;
    private final  String number;

    /**
     * Creates an presigned url to upload a file part to.
     * @param uploadId  id of the upload from the create call
     * @param key key of the upload from the create call
     * @param body body of the file to upload
     * @param number partNumber of this file in upload
     */
    @JsonCreator
    public PrepareUploadPartRequestBody(
            @JsonProperty("uploadId") String uploadId,
            @JsonProperty("key") String key,
            @JsonProperty("body") String body,
            @JsonProperty("number") String number) {
        this.uploadId = uploadId;
        this.key = key;
        this.body = body;
        this.number = number;
    }

    public String getUploadId() {
        return uploadId;
    }

    public String getKey() {
        return key;
    }

    public String getBody() {
        return body;
    }

    public String getNumber() {
        return number;
    }
}
