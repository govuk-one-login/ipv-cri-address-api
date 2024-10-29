package gov.uk.address.api.testharness;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestHarnessResponse {

    private Event event;

    public TestHarnessResponse(Event event) {

        this.event = event;
    }

    public TestHarnessResponse() {}

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
