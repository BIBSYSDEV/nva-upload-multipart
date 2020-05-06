package no.unit.nva.amazon.s3.model;

public class CompleteUploadResponseBody {

    private final String location;

    public CompleteUploadResponseBody(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

}
