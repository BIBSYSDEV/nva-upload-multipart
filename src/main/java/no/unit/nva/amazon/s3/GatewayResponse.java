package no.unit.nva.amazon.s3;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * POJO containing response object for API Gateway.
 */
public class GatewayResponse {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String EMPTY_JSON = "{}";
    public static final String BODY_KEY = "body";
    public static final String ERROR_KEY = "error";

    private String body;
    private transient Map<String, String> headers;
    private int statusCode;

    /**
     * GatewayResponse contains response status, response headers and body with payload resp. error messages.
     */
    public GatewayResponse() {
        this.statusCode = 0;
        this.body = EMPTY_JSON;
        this.headers = new ConcurrentHashMap<>();
    }

    /**
     * GatewayResponse convenience constructor to set response status and body with payload direct.
     */
    public GatewayResponse(final String body, Map<String, String> headers, final int status) {
        this.statusCode = status;
        this.body = body;
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int status) {
        this.statusCode = status;
    }

    /**
     * Set error message as a json string to body.
     *
     * @param message message from exception
     */
    public void setErrorBody(String message) {
        JsonObject json = new JsonObject();
        json.addProperty(ERROR_KEY, message);
        this.body = json.toString();
    }


    @Override
    public String toString() {
        return "GatewayResponse{"
                + "body='" + body + '\''
                + ", headers=" + headers
                + ", statusCode=" + statusCode
                + '}';
    }
}
