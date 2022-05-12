package uk.gov.di.ipv.cri.address.library.domain;

import com.nimbusds.jwt.SignedJWT;
import uk.gov.di.ipv.cri.address.library.persistence.item.PersonIdentity;

import java.net.URI;
import java.util.Date;

public class SessionRequest {
    private String issuer;
    private String subject;
    private String audience;
    private Date expirationTime;
    private Date notBeforeTime;
    private String responseType;
    private String clientId;
    private String jwtClientId;
    private URI redirectUri;
    private String state;
    private SignedJWT signedJWT;
    private PersonIdentity identity;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Date getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Date getNotBeforeTime() {
        return notBeforeTime;
    }

    public void setNotBeforeTime(Date notBeforeTime) {
        this.notBeforeTime = notBeforeTime;
    }

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

    public URI getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(URI redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public SignedJWT getSignedJWT() {
        return signedJWT;
    }

    public void setSignedJWT(SignedJWT signedJWT) {
        this.signedJWT = signedJWT;
    }

    public String getJwtClientId() {
        return jwtClientId;
    }

    public void setJwtClientId(String jwtClientId) {
        this.jwtClientId = jwtClientId;
    }

    public PersonIdentity getPersonIdentity() {
        return identity;
    }

    public void setPersonIdentity(PersonIdentity identity) {
        this.identity = identity;
    }
}
