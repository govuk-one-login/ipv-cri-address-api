package gov.uk.address.api.testharness;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceInformation {
    @JsonProperty("encoded")
    private String encoded;

    public DeviceInformation(String encoded) {
        this.encoded = encoded;
    }

    public DeviceInformation() {}

    public String getEncoded() {
        return encoded;
    }

    public void setEncoded(String encoded) {
        this.encoded = encoded;
    }
}
