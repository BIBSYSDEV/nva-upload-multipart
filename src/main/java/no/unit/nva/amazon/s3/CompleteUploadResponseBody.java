package no.unit.nva.amazon.s3;

public class CompleteUploadResponseBody {

    private final String key;

    public CompleteUploadResponseBody(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
