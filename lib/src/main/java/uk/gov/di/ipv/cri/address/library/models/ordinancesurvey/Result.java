package uk.gov.di.ipv.cri.address.library.models.ordinancesurvey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Result {

    @SerializedName("DPA")
    @Expose
    private Dpa dpa;

    public Dpa getDpa() {
        return dpa;
    }

    public void setDpa(Dpa dpa) {
        this.dpa = dpa;
    }
}
