package gov.uk.address.api.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TxmaEvent {

    private long timestamp;
    private long eventTimestampMs;
    private String eventName;
    private String componentId;
    private User user;

    // Getters and Setters
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

class User {
    private String userId;
    private String ipAddress;
    private String sessionId;
    private String persistentSessionId;
    private String govukSigninJourneyId;

    // Getters and Setters
    @JsonProperty("user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonProperty("ip_address")
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @JsonProperty("session_id")
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @JsonProperty("persistent_session_id")
    public String getPersistentSessionId() {
        return persistentSessionId;
    }

    public void setPersistentSessionId(String persistentSessionId) {
        this.persistentSessionId = persistentSessionId;
    }

    @JsonProperty("govuk_signin_journey_id")
    public String getGovukSigninJourneyId() {
        return govukSigninJourneyId;
    }

    public void setGovukSigninJourneyId(String govukSigninJourneyId) {
        this.govukSigninJourneyId = govukSigninJourneyId;
    }
}
