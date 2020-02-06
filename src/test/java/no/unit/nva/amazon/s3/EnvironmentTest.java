package no.unit.nva.amazon.s3;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class EnvironmentTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private Environment environment;

    @Before
    public void setUp() {
        environment = mock(Environment.class);
    }

    @Test
    public void testDefaultConstructor() {
        final Optional<String> variableNotDefined = new Environment().get("variableNotDefined");
        assertTrue(!variableNotDefined.isPresent());
    }


}
