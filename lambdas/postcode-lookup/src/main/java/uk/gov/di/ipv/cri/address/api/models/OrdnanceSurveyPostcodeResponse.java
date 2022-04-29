package uk.gov.di.ipv.cri.address.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OrdnanceSurveyPostcodeResponse {

    @JsonProperty("header")
    private Header header;

    @JsonProperty("results")
    private List<Result> results = null;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }
}
