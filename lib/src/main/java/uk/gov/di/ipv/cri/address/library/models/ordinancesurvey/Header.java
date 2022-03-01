package uk.gov.di.ipv.cri.address.library.models.ordinancesurvey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Header {

    @SerializedName("uri")
    @Expose
    private String uri;

    @SerializedName("query")
    @Expose
    private String query;

    @SerializedName("offset")
    @Expose
    private Integer offset;

    @SerializedName("totalresults")
    @Expose
    private Integer totalresults;

    @SerializedName("format")
    @Expose
    private String format;

    @SerializedName("dataset")
    @Expose
    private String dataset;

    @SerializedName("lr")
    @Expose
    private String lr;

    @SerializedName("maxresults")
    @Expose
    private Integer maxresults;

    @SerializedName("epoch")
    @Expose
    private String epoch;

    @SerializedName("output_srs")
    @Expose
    private String outputSrs;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Header withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Header withQuery(String query) {
        this.query = query;
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Header withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public Integer getTotalresults() {
        return totalresults;
    }

    public void setTotalresults(Integer totalresults) {
        this.totalresults = totalresults;
    }

    public Header withTotalresults(Integer totalresults) {
        this.totalresults = totalresults;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Header withFormat(String format) {
        this.format = format;
        return this;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public Header withDataset(String dataset) {
        this.dataset = dataset;
        return this;
    }

    public String getLr() {
        return lr;
    }

    public void setLr(String lr) {
        this.lr = lr;
    }

    public Header withLr(String lr) {
        this.lr = lr;
        return this;
    }

    public Integer getMaxresults() {
        return maxresults;
    }

    public void setMaxresults(Integer maxresults) {
        this.maxresults = maxresults;
    }

    public Header withMaxresults(Integer maxresults) {
        this.maxresults = maxresults;
        return this;
    }

    public String getEpoch() {
        return epoch;
    }

    public void setEpoch(String epoch) {
        this.epoch = epoch;
    }

    public Header withEpoch(String epoch) {
        this.epoch = epoch;
        return this;
    }

    public String getOutputSrs() {
        return outputSrs;
    }

    public void setOutputSrs(String outputSrs) {
        this.outputSrs = outputSrs;
    }

    public Header withOutputSrs(String outputSrs) {
        this.outputSrs = outputSrs;
        return this;
    }
}
