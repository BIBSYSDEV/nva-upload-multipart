package no.unit.nva.fileupload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.util.Objects.requireNonNull;

public class CompleteUploadPart {

    private final String etag;
    private final Integer partNumber;

    @JsonCreator
    public CompleteUploadPart(
            @JsonProperty("PartNumber") Integer partNumber,
            @JsonProperty("ETag") String etag) {
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
