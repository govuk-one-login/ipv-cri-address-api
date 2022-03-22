package uk.gov.di.ipv.cri.address.library.models;

import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.net.URI;
import java.util.UUID;

public class AuthorizationResponse {
    private UUID code;
    private URI redirect_uri;
    private String state;

    public AuthorizationResponse(AddressSessionItem session) {
        this.code = session.getAuthorizationCode();
        this.redirect_uri = session.getRedirectUri();
        this.state = session.getState();
    }

    public UUID getCode() {
        return code;
    }

    public URI getRedirect_uri() {
        return redirect_uri;
    }

    public String getState() {
        return state;
    }
}
