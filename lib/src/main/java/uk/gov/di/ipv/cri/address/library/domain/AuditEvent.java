package uk.gov.di.ipv.cri.address.library.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditEvent {

    @JsonProperty("timestamp")
    private int timestamp;

    @JsonProperty("event_name")
    private AuditEventTypes event;

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

    public String getTimestampFormatted() {
        return timestampFormatted;
    }

    public void setTimestampFormatted(String timestampFormatted) {
        this.timestampFormatted = timestampFormatted;
    }
}
