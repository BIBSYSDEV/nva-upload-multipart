package no.unit.nva.amazon.s3;

public class ParameterMissingException  extends  RuntimeException {

    private static String MESSAGE_TEMPLATE = "Parameter %s is missing";

    public ParameterMissingException(String parameterName) {
        super(String.format(MESSAGE_TEMPLATE, parameterName));
    }
}
