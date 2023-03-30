package no.unit.nva.fileupload;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public class SignedPartResponseBody implements JsonSerializable {

    @JsonProperty("url")
    private final URI url;

    @JsonProperty("headers")
    private final Map<String, String> headers;

    public SignedPartResponseBody(@JsonProperty("url") URI url, @JsonProperty("headers") Map<String, String> headers) {
        this.headers = headers;
        this.url = url;
    }

    @JacocoGenerated
    public URI getUrl() {
        return url;
    }

    @JacocoGenerated
    public Map<String, String> getHeaders() {
        return headers;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getUrl(), getHeaders());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SignedPartResponseBody)) {
            return false;
        }
        SignedPartResponseBody that = (SignedPartResponseBody) o;
        return Objects.equals(getUrl(), that.getUrl())
               && Objects.equals(getHeaders(), that.getHeaders());
    }
}
