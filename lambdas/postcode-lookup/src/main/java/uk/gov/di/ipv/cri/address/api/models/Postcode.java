package uk.gov.di.ipv.cri.address.api.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Postcode {

    @JsonProperty("postcode")
    private String value;

    @JsonCreator
    public Postcode(@JsonProperty("postcode") String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
