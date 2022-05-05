package uk.gov.di.ipv.cri.address.library.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.cri.address.library.persistence.item.SessionItem;

import java.net.URI;

public class AuthorizationResponse {
    private final String code;
    private final URI redirectUri;
    private final String state;

    public AuthorizationResponse(SessionItem session) {
        this.code = session.getAuthorizationCode();
        this.redirectUri = session.getRedirectUri();
        this.state = session.getState();
    }

    public String getCode() {
        return code;
    }

    public String getState() {
        return state;
    }

    @JsonProperty("redirect_uri")
    public URI getRedirectUri() {
        return redirectUri;
    }
}
