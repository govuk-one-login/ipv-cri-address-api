package uk.gov.di.ipv.cri.address.library.models.ordinancesurvey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OrdinanceSurveyPostcodeResponse {

    /** (Required) */
    @SerializedName("header")
    @Expose
    private Header header;
    /** (Required) */
    @SerializedName("results")
    @Expose
    private List<Result> results = null;

    /** (Required) */
    public Header getHeader() {
        return header;
    }

    /** (Required) */
    public void setHeader(Header header) {
        this.header = header;
    }

    /** (Required) */
    public List<Result> getResults() {
        return results;
    }

    /** (Required) */
    public void setResults(List<Result> results) {
        this.results = results;
    }
}
