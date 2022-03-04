package uk.gov.di.ipv.cri.address.library.models.ordinancesurvey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Result {

    /** (Required) */
    @SerializedName("DPA")
    @Expose
    private Dpa dpa;

    /** (Required) */
    public Dpa getDpa() {
        return dpa;
    }

    /** (Required) */
    public void setDpa(Dpa dpa) {
        this.dpa = dpa;
    }
}
