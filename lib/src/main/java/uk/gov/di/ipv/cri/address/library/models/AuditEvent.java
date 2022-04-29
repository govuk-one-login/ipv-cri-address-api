package uk.gov.di.ipv.cri.address.library.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.cri.address.library.domain.AuditEventTypes;

public class AuditEvent {

    @JsonProperty("timestamp")
    private int timestamp;

    @JsonProperty("event_name")
    private AuditEventTypes event;

    @JsonProperty("event_id")
    private String event_id;

    @JsonProperty("client_id")
    private String client_id;

    @JsonProperty("timestamp_formatted")
    private String timestamp_formatted;

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public AuditEventTypes getEvent() {
        return event;
    }

    public void setEvent(AuditEventTypes event) {
        this.event = event;
    }

    public String getEvent_id() {
        return event_id;
    }

    public void setEvent_id(String event_id) {
        this.event_id = event_id;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getTimestamp_formatted() {
        return timestamp_formatted;
    }

    public void setTimestamp_formatted(String timestamp_formatted) {
        this.timestamp_formatted = timestamp_formatted;
    }
}
