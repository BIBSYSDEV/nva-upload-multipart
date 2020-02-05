package no.unit.nva.amazon.s3;

import com.google.gson.annotations.SerializedName;

public class CompleteUploadPart {
    @SerializedName("ETag")
    private final String etag;
    @SerializedName("PartNumber")
    private final String partNumber;

    public CompleteUploadPart(String partNumber, String etag) {
        this.partNumber = partNumber;
        this.etag = etag;
    }

    public String getEtag() {
        return etag;
    }

    public String getPartNumber() {
        return partNumber;
    }

}
