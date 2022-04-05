package uk.gov.di.ipv.cri.address.library.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;
import java.util.StringJoiner;

public class SessionRequest {

    @JsonAlias("response_type")
    private String responseType;

    @JsonAlias("client_id")
    private String clientId;

    @JsonAlias("state")
    private String state;

    @JsonAlias("redirect_uri")
    private URI redirectUri;

    @JsonAlias("request")
    private String requestJWT;

    @JsonIgnore private transient String subject;

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public URI getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(URI redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getRequestJWT() {
        return requestJWT;
    }

    public void setRequestJWT(String requestJWT) {
        this.requestJWT = requestJWT;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SessionRequest.class.getSimpleName() + "[", "]")
                .add("responseType='" + responseType + "'")
                .add("clientId='" + clientId + "'")
                .add("state='" + state + "'")
                .add("redirectUri=" + redirectUri)
                .add("requestJWT='<>'")
                .toString();
    }
}
