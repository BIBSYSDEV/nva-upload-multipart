package no.unit.nva.amazon.s3;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class CompleteUploadPart {
    @SerializedName("ETag")
    private final String etag;
    @SerializedName("PartNumber")
    private final int partNumber;

    public CompleteUploadPart(int partNumber, String etag) {
        this.partNumber = partNumber;
        this.etag = etag;
    }

    public String getEtag() {
        return etag;
    }

    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Checks if a part has value.
     * @param completeUploadPart part to check
     * @return true if given part has value
     */
    public static boolean hasValue(CompleteUploadPart completeUploadPart) {
        boolean notEnoughData  = Objects.isNull(completeUploadPart)
                || Objects.isNull(completeUploadPart.getEtag())
                || completeUploadPart.getEtag().isEmpty();
        return !notEnoughData;
    }
}
