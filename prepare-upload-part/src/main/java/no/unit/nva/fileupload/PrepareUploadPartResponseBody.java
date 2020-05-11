package no.unit.nva.fileupload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;

public class PrepareUploadPartResponseBody {

    private final URL url;

    @JsonCreator
    public PrepareUploadPartResponseBody(@JsonProperty("url") URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }
}
