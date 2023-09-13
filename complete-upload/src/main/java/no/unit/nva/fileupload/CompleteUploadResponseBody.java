package no.unit.nva.fileupload;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record CompleteUploadResponseBody(String location, String identifier, String fileName, String mimeType,
                                         long size) {

    public static final class Builder {

        private String location;
        private String identifier;
        private String fileName;
        private String mimeType;
        private long size;

        public Builder() {
        }

        public Builder withLocation(String location) {
            return withIdentifier(location);
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            this.location = identifier;
            return this;
        }

        public Builder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder withMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder withSize(long size) {
            this.size = size;
            return this;
        }

        public CompleteUploadResponseBody build() {
            return new CompleteUploadResponseBody(location, identifier, fileName, mimeType, size);
        }
    }
}
