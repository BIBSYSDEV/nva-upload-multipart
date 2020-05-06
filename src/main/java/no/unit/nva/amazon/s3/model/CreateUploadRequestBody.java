package no.unit.nva.amazon.s3.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateUploadRequestBody {

    private final String filename;
    private final String size;
    private final String mimetype;
    private String md5hash;

    /**
     * Creates a request to upload a file to S3.
     *
     * @param filename name of the file  to upload
     * @param size     size fo the file to upload
     * @param mimetype mimetype of the uploaded file
     */
    @JsonCreator
    public CreateUploadRequestBody(@JsonProperty("filename") String filename,
                                   @JsonProperty("size") String size,
                                   @JsonProperty("mimetype") String mimetype) {
        this.filename = filename;
        this.size = size;
        this.mimetype = mimetype;
    }


    public String getFilename() {
        return filename;
    }

    public String getSize() {
        return size;
    }


    public String getMimetype() {
        return mimetype;
    }


    public void setMd5hash(String md5hash) {
        this.md5hash = md5hash;
    }

    public String getMd5hash() {
        return md5hash;
    }

}
