package gov.uk.address.api.testharness;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
    @JsonProperty("S")
    private String data;

    public Event(String data) {
        this.data = data;
    }

    public Event() {}

    public String getData() {
        return data;
    }

    public void setData(String s) {
        this.data = s;
    }
}
