package no.unit.nva.amazon.s3;

import com.amazonaws.services.s3.model.PartSummary;

public class ListPartsElement {

    private String partNumber;
    private String size;
    private String eTag;

    public ListPartsElement(PartSummary partSummary) {
        this.partNumber = Integer.toString(partSummary.getPartNumber());
        this.size = Long.toString(partSummary.getSize());
        this.eTag = partSummary.getETag();
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }
}
