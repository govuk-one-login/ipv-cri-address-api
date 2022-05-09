package uk.gov.di.ipv.cri.address.library.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class AuditEvent {

    @JsonProperty("timestamp")
    private Date timestamp;

    @JsonProperty("event_name")
    private AuditEventTypes event;

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public AuditEventTypes getEvent() {
        return event;
    }

    public void setEvent(AuditEventTypes event) {
        this.event = event;
    }
}
