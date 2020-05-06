package no.unit.nva.amazon.s3.model;

import com.amazonaws.services.s3.model.PartSummary;

public class ListPartsElement {

    private String partNumber;
    private String size;
    private String etag;

    /**
     * Contains parts information aboud upload, eTag and position.
     * @param partSummary list of eTags and partsnumber
     */
    public ListPartsElement(PartSummary partSummary) {
        this.partNumber = Integer.toString(partSummary.getPartNumber());
        this.size = Long.toString(partSummary.getSize());
        this.etag = partSummary.getETag();
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

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}
