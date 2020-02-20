package no.unit.nva.amazon.s3;

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
    public PrepareUploadPartRequestBody(String uploadId, String key, String body, String number) {
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
