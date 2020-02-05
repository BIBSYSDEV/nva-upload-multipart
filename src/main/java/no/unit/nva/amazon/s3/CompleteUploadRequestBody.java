package no.unit.nva.amazon.s3;

import java.util.List;

public class CompleteUploadRequestBody {

    public String uploadId;
    public String key;
    public List<CompleteUploadPart> parts;


}
