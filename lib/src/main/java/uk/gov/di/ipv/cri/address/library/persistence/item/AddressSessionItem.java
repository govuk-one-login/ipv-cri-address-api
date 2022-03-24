package uk.gov.di.ipv.cri.address.library.persistence.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import uk.gov.di.ipv.cri.address.library.helpers.ListOfMapConverter;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@DynamoDbBean
public class AddressSessionItem {
    public static final String AUTHORIZATION_CODE_INDEX = "authorizationCode-index";
    private UUID sessionId;
    private long expiryDate;
    private String clientId;
    private String state;
    private URI redirectUri;
    private List<CanonicalAddressWithResidency> addresses = new ArrayList<>();
    private UUID authorizationCode;

    private String accessToken;

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

    @DynamoDbSecondaryPartitionKey(indexNames = AUTHORIZATION_CODE_INDEX)
    public UUID getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(UUID authorizationCode) {
        this.authorizationCode = authorizationCode;
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

    @DynamoDbConvertedBy(ListOfMapConverter.class)
    public List<CanonicalAddressWithResidency> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<CanonicalAddressWithResidency> addresses) {
        this.addresses = Objects.requireNonNullElseGet(addresses, ArrayList::new);
    }
}
