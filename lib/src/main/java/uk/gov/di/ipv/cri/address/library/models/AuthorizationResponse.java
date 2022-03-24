package uk.gov.di.ipv.cri.address.library.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.net.URI;
import java.util.UUID;

public class AuthorizationResponse {
    private final UUID code;
    private final URI redirectUri;
    private final String state;

    public AuthorizationResponse(AddressSessionItem session) {
        this.code = session.getAuthorizationCode();
        this.redirectUri = session.getRedirectUri();
        this.state = session.getState();
    }

    public UUID getCode() {
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
