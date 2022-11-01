package uk.gov.di.ipv.cri.address.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Header {

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("query")
    private String query;

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("totalresults")
    private Integer totalresults;

    @JsonProperty("format")
    private String format;

    @JsonProperty("dataset")
    private String dataset;

    @JsonProperty("lr")
    private String lr;

    @JsonProperty("maxresults")
    private Integer maxresults;

    @JsonProperty("epoch")
    private String epoch;

    @JsonProperty("output_srs")
    private String outputSrs;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getTotalresults() {
        return totalresults;
    }

    public void setTotalresults(Integer totalresults) {
        this.totalresults = totalresults;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getLr() {
        return lr;
    }

    public void setLr(String lr) {
        this.lr = lr;
    }

    public Integer getMaxresults() {
        return maxresults;
    }

    public void setMaxresults(Integer maxresults) {
        this.maxresults = maxresults;
    }

    public String getEpoch() {
        return epoch;
    }

    public void setEpoch(String epoch) {
        this.epoch = epoch;
    }

    public String getOutputSrs() {
        return outputSrs;
    }

    public void setOutputSrs(String outputSrs) {
        this.outputSrs = outputSrs;
    }
}
