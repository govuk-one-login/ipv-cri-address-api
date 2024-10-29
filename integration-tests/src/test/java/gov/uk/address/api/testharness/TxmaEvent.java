package gov.uk.address.api.testharness;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TxmaEvent {
    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("event_timestamp_ms")
    private long eventTimestampMs;

    @JsonProperty("event_name")
    private String eventName;

    @JsonProperty("component_id")
    private String componentId;

    @JsonProperty("restricted")
    private Restricted restricted;

    private User user;

    public TxmaEvent(
            long timestamp,
            long eventTimestampMs,
            String eventName,
            String componentId,
            Restricted restricted,
            User user) {
        this.timestamp = timestamp;
        this.eventTimestampMs = eventTimestampMs;
        this.eventName = eventName;
        this.componentId = componentId;
        this.restricted = restricted;
        this.user = user;
    }

    public TxmaEvent() {}

    public Restricted getRestricted() {
        return restricted;
    }

    public void setRestricted(Restricted restricted) {
        this.restricted = restricted;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getEventTimestampMs() {
        return eventTimestampMs;
    }

    public void setEventTimestampMs(long eventTimestampMs) {
        this.eventTimestampMs = eventTimestampMs;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
