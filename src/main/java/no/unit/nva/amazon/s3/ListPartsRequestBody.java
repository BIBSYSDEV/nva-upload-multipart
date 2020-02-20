package no.unit.nva.amazon.s3;

public class ListPartsRequestBody {

    private final String uploadId;
    private final String key;

    public ListPartsRequestBody(String uploadId, String key) {
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
