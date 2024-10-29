package gov.uk.address.api.testharness;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
    private String userId;
    private String ipAddress;
    private String sessionId;
    private String persistentSessionId;
    private String govukSigninJourneyId;

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
