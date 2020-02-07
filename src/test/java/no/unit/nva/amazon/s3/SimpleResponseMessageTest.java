package no.unit.nva.amazon.s3;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleResponseMessageTest {


    public static final String MESSAGE = "Some message";

    @Test
    public void testMessageAssigned() {
        SimpleMessageResponse response = new SimpleMessageResponse(MESSAGE);
        assertEquals(MESSAGE, response.getMessage());
    }
}

