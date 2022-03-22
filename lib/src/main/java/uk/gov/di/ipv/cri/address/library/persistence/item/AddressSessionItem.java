package uk.gov.di.ipv.cri.address.library.persistence.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.net.URI;
import java.util.UUID;

@DynamoDbBean
public class AddressSessionItem {
    public static final String AUTHORIZATION_CODE_INDEX = "authorizationCode-index";
    private UUID sessionId;
    private long expiryDate;
    private String clientId;
    private String state;
    private URI redirectUri;

    private String accessToken;

    private String authorizationCode;

    public AddressSessionItem() {
        sessionId = UUID.randomUUID();
    }

    @DynamoDbPartitionKey()
    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setRedirectUri(URI redirectUri) {
        this.redirectUri = redirectUri;
    }

    public URI getRedirectUri() {
        return redirectUri;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = AUTHORIZATION_CODE_INDEX)
    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }
}
