package no.unit.nva.amazon.s3.model;

public class AbortMultipartUploadRequestBody {

    private String uploadId;
    private String key;

    public AbortMultipartUploadRequestBody(String uploadId, String key) {
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
