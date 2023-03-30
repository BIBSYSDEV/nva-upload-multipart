package no.unit.nva.fileupload;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Test;

public class FetchSignPartHandlerTest {

    public static final String TEST_BUCKET_NAME = "bucketName";
    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_TYPE = JsonUtils.dtoObjectMapper.getTypeFactory()
                                                                           .constructParametricType(
                                                                               GatewayResponse.class,
                                                                               SignedPartResponseBody.class);
    private static final String ACCEPT = "Accept";
    private final Context CONTEXT = mock(Context.class);
    private AmazonS3Client s3client;
    private ByteArrayOutputStream output;
    private FetchSignPartHandler handler;

    @Before
    public void setup() {
        s3client = mock(AmazonS3Client.class);
        output = new ByteArrayOutputStream();
        handler = new FetchSignPartHandler(new Environment(), s3client, TEST_BUCKET_NAME);
    }

    @Test
    public void dummyTest() throws IOException {
        try (var input = generateHandlerRequest(randomString(), randomString(), randomString())) {
            handler.handleRequest(input, output, CONTEXT);
            var response = parseHandlerResponse();
            assertThat(response.getStatusCode(), is(equalTo(SC_OK)));
            var body = response.getBodyObject(SignedPartResponseBody.class);
            assertThat(body, is(equalTo(new SignedPartResponseBody(null, null))));
        }
    }

    private GatewayResponse<SignedPartResponseBody> parseHandlerResponse() throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(output.toString(), PARAMETERIZED_GATEWAY_RESPONSE_TYPE);
    }

    private InputStream generateHandlerRequest(String uploadIdentifier, String partNumber, String filename, Map<String,
                                                                                                                   String> headers)
        throws JsonProcessingException {
        var pathParameters = Map.of("uploadIdentifier", uploadIdentifier, "partNumber", partNumber);
        var queryParams = Map.of("key", filename);
        return new HandlerRequestBuilder<InputStream>(JsonUtils.dtoObjectMapper)
                   .withHeaders(headers)
                   .withPathParameters(pathParameters)
                   .withQueryParameters(queryParams)
                   .build();
    }

    private InputStream generateHandlerRequest(String uploadIdentifier, String partNumber, String filename)
        throws JsonProcessingException {
        Map<String, String> headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        return generateHandlerRequest(uploadIdentifier, partNumber, filename, headers);
    }
}
