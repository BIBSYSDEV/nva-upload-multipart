package no.unit.nva.amazon.s3;

import java.util.List;

public class CompleteUploadRequestBody {

    private String uploadId;
    private String key;
    private List<CompleteUploadPart> parts;


    /**
     * Creates an request to complete aa S3 upload.
     * @param uploadId  identifier of the upload from create
     * @param key bucket key for identify s3 object
     * @param parts list of uploaded pars index and eTag
     */
    public CompleteUploadRequestBody(String uploadId, String key, List<CompleteUploadPart> parts) {
        this.uploadId = uploadId;
        this.key = key;
        this.parts = parts;
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

    public List<CompleteUploadPart> getParts() {
        return parts;
    }

    public void setParts(List<CompleteUploadPart> parts) {
        this.parts = parts;
    }
}
