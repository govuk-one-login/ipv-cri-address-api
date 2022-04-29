package uk.gov.di.ipv.cri.address.api.domain;

import com.fasterxml.jackson.annotation.JsonAlias;

public class RawSessionRequest {
    @JsonAlias("client_id")
    private String clientId;

    @JsonAlias("request")
    private String requestJWT;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRequestJWT() {
        return requestJWT;
    }

    public void setRequestJWT(String requestJWT) {
        this.requestJWT = requestJWT;
    }
}
