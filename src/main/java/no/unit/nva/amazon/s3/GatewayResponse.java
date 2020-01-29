package no.unit.nva.amazon.s3;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * POJO containing response object for API Gateway.
 */
public class GatewayResponse {

    public static final String CORS_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    public static final String ERROR_KEY = "error";
    private final String body;
    private transient Map<String, String> headers;
    private final int statusCode;

    /**
     * GatewayResponse constructor.
     */
    public GatewayResponse(final String body, final int status, String contentType) {
        this.statusCode = status;
        this.body = body;
        generateHeaders(contentType);
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    private void generateHeaders(String contentType) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, contentType);
        final String corsAllowDomain = Config.getInstance().getCorsHeader();
        if (StringUtils.isNotEmpty(corsAllowDomain)) {
            headers.put(CORS_ALLOW_ORIGIN_HEADER, corsAllowDomain);
        }
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
    }

    /**
     * Create error GatewayResponse.
     *
     * @param message   message
     * @param statusCode    statusCode
     * @return  GatewayResponse
     */
    public static GatewayResponse errorGatewayResponse(String message, int statusCode) {
        JsonObject json = new JsonObject();
        json.addProperty(ERROR_KEY, message);
        return new GatewayResponse(json.toString(), statusCode, MediaType.APPLICATION_JSON);
    }


}
