package no.unit.nva.amazon.s3;

import java.net.URL;

public class PrepareUploadPartResponseBody {

    private final URL url;

    public PrepareUploadPartResponseBody(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }
}
