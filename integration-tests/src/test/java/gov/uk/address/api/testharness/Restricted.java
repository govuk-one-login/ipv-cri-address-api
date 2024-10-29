package gov.uk.address.api.testharness;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Restricted {
    @JsonProperty("device_information")
    private DeviceInformation deviceInformation;

    public Restricted(DeviceInformation deviceInformation) {
        this.deviceInformation = deviceInformation;
    }

    public Restricted() {}

    public DeviceInformation getDeviceInformation() {
        return deviceInformation;
    }

    public void setDeviceInformation(DeviceInformation deviceInformation) {
        this.deviceInformation = deviceInformation;
    }
}
