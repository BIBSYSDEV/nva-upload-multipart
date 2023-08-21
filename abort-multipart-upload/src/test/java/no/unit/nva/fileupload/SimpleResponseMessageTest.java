package no.unit.nva.fileupload;


import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SimpleResponseMessageTest {


    public static final String MESSAGE = "Some message";

    @Test
    void testMessageAssigned() {
        SimpleMessageResponse response = new SimpleMessageResponse(MESSAGE);
        assertThat(MESSAGE, is(equalTo(response.getMessage())));
    }
}

