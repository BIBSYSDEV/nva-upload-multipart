package no.unit.nva.amazon.s3;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.Optional;

import static no.unit.nva.amazon.s3.Environment.ALLOWED_ORIGIN_KEY;
import static no.unit.nva.amazon.s3.Environment.S3_UPLOAD_BUCKET_KEY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class HandlerEnvironmentTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private Environment environment;

    @Before
    public void setUp() {
        environment = mock(Environment.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAbortHandlerDefaultConstructorMissingAllowedOrigin() {
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY,S3_UPLOAD_BUCKET_KEY);
        AbortMultipartUploadHandler abortMultipartUploadHandler = new AbortMultipartUploadHandler();
        assertNotNull(abortMultipartUploadHandler);
        fail("no exception ");
    }

    @Test(expected = IllegalStateException.class)
    public void testAbortHandlerDefaultConstructorMissingS3UploadBucket() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY,ALLOWED_ORIGIN_KEY);
        AbortMultipartUploadHandler abortMultipartUploadHandler = new AbortMultipartUploadHandler();
        assertNotNull(abortMultipartUploadHandler);
        fail("no exception ");
    }


    @Test(expected = IllegalStateException.class)
    public void testCompleteHandlerDefaultConstructorMissingAllowedOrigin() {
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY,S3_UPLOAD_BUCKET_KEY);
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler();
        assertNotNull(completeUploadHandler);
        fail("no exception ");
    }

    @Test(expected = IllegalStateException.class)
    public void testComplerteHandlerDefaultConstructorMissingS3UploadBucket() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY,ALLOWED_ORIGIN_KEY);
        CompleteUploadHandler completeUploadHandler = new CompleteUploadHandler();
        assertNotNull(completeUploadHandler);
        fail("no exception ");
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateHandlerDefaultConstructorMissingAllowedOrigin() {
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY,S3_UPLOAD_BUCKET_KEY);
        CreateUploadHandler createUploadHandler = new CreateUploadHandler();
        assertNotNull(createUploadHandler);
        fail("no exception ");
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateHandlerDefaultConstructorMissingS3UploadBucket() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY,ALLOWED_ORIGIN_KEY);
        CreateUploadHandler createUploadHandler = new CreateUploadHandler();
        assertNotNull(createUploadHandler);
        fail("no exception ");
    }

    @Test(expected = IllegalStateException.class)
    public void testListPartsHandlerDefaultConstructorMissingAllowedOrigin() {
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY,S3_UPLOAD_BUCKET_KEY);
        ListPartsHandler listPartsHandler = new ListPartsHandler();
        assertNotNull(listPartsHandler);
        fail("no exception ");
    }

    @Test(expected = IllegalStateException.class)
    public void testListPartsHandlerDefaultConstructorMissingS3UploadBucket() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY,ALLOWED_ORIGIN_KEY);
        ListPartsHandler listPartsHandler = new ListPartsHandler();
        assertNotNull(listPartsHandler);
        fail("no exception ");
    }

    @Test(expected = IllegalStateException.class)
    public void testPrepareUploadPartsHandlerDefaultConstructorMissingAllowedOrigin() {
        environmentVariables.set(S3_UPLOAD_BUCKET_KEY,S3_UPLOAD_BUCKET_KEY);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler();
        assertNotNull(prepareUploadPartHandler);
        fail("no exception ");
    }

    @Test(expected = IllegalStateException.class)
    public void testPrepareUploadPartsHandlerDefaultConstructorMissingS3UploadBucket() {
        environmentVariables.set(ALLOWED_ORIGIN_KEY,ALLOWED_ORIGIN_KEY);
        PrepareUploadPartHandler prepareUploadPartHandler = new PrepareUploadPartHandler();
        assertNotNull(prepareUploadPartHandler);
        fail("no exception ");
    }





}

