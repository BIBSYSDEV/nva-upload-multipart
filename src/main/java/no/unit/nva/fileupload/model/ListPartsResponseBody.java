package no.unit.nva.fileupload.model;

import java.util.ArrayList;
import java.util.List;

public class ListPartsResponseBody extends ArrayList<ListPartsElement> {

    /**
     * Default constructor for ListPartsResponseBody.
     */
    public ListPartsResponseBody() {
        super();
    }

    /**
     * Create ListPartsResponseBody of a list of listParts.
     *
     * @param listParts listParts
     * @return  ListPartsResponseBody
     */
    public static ListPartsResponseBody of(List<ListPartsElement> listParts) {
        ListPartsResponseBody listPartsResponseBody = new ListPartsResponseBody();
        listPartsResponseBody.addAll(listParts);
        return listPartsResponseBody;
    }
}
