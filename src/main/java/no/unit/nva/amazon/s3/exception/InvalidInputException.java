package no.unit.nva.amazon.s3.exception;

import nva.commons.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;

public class InvalidInputException extends ApiGatewayException {

    public InvalidInputException(Exception e) {
        super(e, "Invalid input: " + e.getMessage());
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_BAD_REQUEST;
    }
}
