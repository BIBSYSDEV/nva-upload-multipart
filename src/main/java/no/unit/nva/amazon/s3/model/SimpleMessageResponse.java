package no.unit.nva.amazon.s3.model;

public class SimpleMessageResponse {

    private final String message;

    public SimpleMessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
