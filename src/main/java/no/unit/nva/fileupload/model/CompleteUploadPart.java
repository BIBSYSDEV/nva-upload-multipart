package no.unit.nva.fileupload.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import static java.util.Objects.requireNonNull;

public class CompleteUploadPart {
    @SerializedName("ETag")
    private final String etag;
    @SerializedName("PartNumber")
    private final Integer partNumber;

    @JsonCreator
    public CompleteUploadPart(
            @JsonProperty("partNumber") Integer partNumber,
            @JsonProperty("etag") String etag) {
        this.partNumber = partNumber;
        this.etag = etag;
    }

    public String getEtag() {
        return etag;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    /**
     *  Check if properties have values.
     *
     * @return  true if all properties have values
     */
    public boolean hasValue() {
        try {
            requireNonNull(etag);
            requireNonNull(partNumber);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
