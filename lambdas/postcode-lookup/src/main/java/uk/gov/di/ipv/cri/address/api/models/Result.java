package uk.gov.di.ipv.cri.address.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Result {

    @JsonProperty("DPA")
    private Dpa dpa;

    public Dpa getDpa() {
        return dpa;
    }

    public void setDpa(Dpa dpa) {
        this.dpa = dpa;
    }
}
