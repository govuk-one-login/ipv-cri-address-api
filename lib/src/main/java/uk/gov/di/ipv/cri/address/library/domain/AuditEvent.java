package uk.gov.di.ipv.cri.address.library.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditEvent {

    @JsonProperty("timestamp")
    private int timestamp;

    @JsonProperty("event_name")
    private AuditEventTypes event;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("timestamp_formatted")
    private String timestampFormatted;

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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTimestampFormatted() {
        return timestampFormatted;
    }

    public void setTimestampFormatted(String timestampFormatted) {
        this.timestampFormatted = timestampFormatted;
    }
}
