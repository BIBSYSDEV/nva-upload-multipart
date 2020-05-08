package no.unit.nva.fileupload.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CompleteUploadResponseBody {

    private final String location;

    @JsonCreator
    public CompleteUploadResponseBody(@JsonProperty("location") String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

}
