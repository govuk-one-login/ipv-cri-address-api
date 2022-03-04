package uk.gov.di.ipv.cri.address.library.models.ordinancesurvey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Header {

    /** (Required) */
    @SerializedName("uri")
    @Expose
    private String uri;
    /** (Required) */
    @SerializedName("query")
    @Expose
    private String query;
    /** (Required) */
    @SerializedName("offset")
    @Expose
    private Integer offset;
    /** (Required) */
    @SerializedName("totalresults")
    @Expose
    private Integer totalresults;
    /** (Required) */
    @SerializedName("format")
    @Expose
    private String format;
    /** (Required) */
    @SerializedName("dataset")
    @Expose
    private String dataset;
    /** (Required) */
    @SerializedName("lr")
    @Expose
    private String lr;
    /** (Required) */
    @SerializedName("maxresults")
    @Expose
    private Integer maxresults;
    /** (Required) */
    @SerializedName("epoch")
    @Expose
    private String epoch;
    /** (Required) */
    @SerializedName("output_srs")
    @Expose
    private String outputSrs;

    /** (Required) */
    public String getUri() {
        return uri;
    }

    /** (Required) */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /** (Required) */
    public String getQuery() {
        return query;
    }

    /** (Required) */
    public void setQuery(String query) {
        this.query = query;
    }

    /** (Required) */
    public Integer getOffset() {
        return offset;
    }

    /** (Required) */
    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    /** (Required) */
    public Integer getTotalresults() {
        return totalresults;
    }

    /** (Required) */
    public void setTotalresults(Integer totalresults) {
        this.totalresults = totalresults;
    }

    /** (Required) */
    public String getFormat() {
        return format;
    }

    /** (Required) */
    public void setFormat(String format) {
        this.format = format;
    }

    /** (Required) */
    public String getDataset() {
        return dataset;
    }

    /** (Required) */
    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    /** (Required) */
    public String getLr() {
        return lr;
    }

    /** (Required) */
    public void setLr(String lr) {
        this.lr = lr;
    }

    /** (Required) */
    public Integer getMaxresults() {
        return maxresults;
    }

    /** (Required) */
    public void setMaxresults(Integer maxresults) {
        this.maxresults = maxresults;
    }

    /** (Required) */
    public String getEpoch() {
        return epoch;
    }

    /** (Required) */
    public void setEpoch(String epoch) {
        this.epoch = epoch;
    }

    /** (Required) */
    public String getOutputSrs() {
        return outputSrs;
    }

    /** (Required) */
    public void setOutputSrs(String outputSrs) {
        this.outputSrs = outputSrs;
    }
}
