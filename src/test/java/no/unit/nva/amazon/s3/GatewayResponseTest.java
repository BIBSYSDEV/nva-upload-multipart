package no.unit.nva.amazon.s3;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class GatewayResponseTest {

    private static final String EMPTY_STRING = "";
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    public static final String CORS_HEADER = "CORS header";
    public static final String MOCK_BODY = "mock";
    public static final String ERROR_BODY = "error";
    public static final String ERROR_JSON = "{\"error\":\"error\"}";

    @Test
    public void testErrorResponse() {
        String expectedJson = ERROR_JSON;
        // calling real constructor (no need to mock as this is not talking to the internet)
        // but helps code coverage
        GatewayResponse gatewayResponse = new GatewayResponse(MOCK_BODY, null,
                Response.Status.CREATED.getStatusCode());
        gatewayResponse.setErrorBody(ERROR_BODY);
        assertEquals(expectedJson, gatewayResponse.getBody());
    }

    @Test
    public void testNoCorsHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        GatewayResponse gatewayResponse = new GatewayResponse(MOCK_BODY, headers,
                Response.Status.CREATED.getStatusCode());
        assertFalse(gatewayResponse.getHeaders().containsKey(GatewayResponse.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertFalse(gatewayResponse.getHeaders().containsValue(CORS_HEADER));
        headers.put(GatewayResponse.ACCESS_CONTROL_ALLOW_ORIGIN, CORS_HEADER);

        GatewayResponse gatewayResponse1 = new GatewayResponse(MOCK_BODY, headers,
                Response.Status.CREATED.getStatusCode());
        assertTrue(gatewayResponse1.getHeaders().containsKey(GatewayResponse.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

}
