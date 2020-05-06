package no.unit.nva.amazon.s3;

import com.amazonaws.services.s3.AmazonS3;
import no.unit.nva.amazon.s3.util.DebugUtils;
import no.unit.nva.amazon.s3.util.Environment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;

import java.util.Optional;

import static no.unit.nva.amazon.s3.util.Environment.AWS_REGION_KEY;
import static no.unit.nva.amazon.s3.util.Environment.DEFAULT_AWS_REGION;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

    @Test
    public void testDebugUtils() {
        final DebugUtils debugUtils = new DebugUtils();
        assertNotNull(debugUtils);
    }

    @Test
    public void testGetS3Client() {
        environmentVariables.set(AWS_REGION_KEY, DEFAULT_AWS_REGION);
        Environment environment = Mockito.spy(new Environment());
        Mockito.when(environment.get(AWS_REGION_KEY)).thenReturn(Optional.of(DEFAULT_AWS_REGION));
        AmazonS3 amazonS3Client = environment.createAmazonS3Client();
        assertNotNull(amazonS3Client);
    }




}

