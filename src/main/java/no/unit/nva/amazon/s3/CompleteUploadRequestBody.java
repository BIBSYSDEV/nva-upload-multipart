package no.unit.nva.amazon.s3;

import com.amazonaws.services.s3.model.PartETag;

import java.util.List;

public class CompleteUploadRequestBody {

    public String uploadId;
    public String key;
    public List<PartETag> partETags;


}
