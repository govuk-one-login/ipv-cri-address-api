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
