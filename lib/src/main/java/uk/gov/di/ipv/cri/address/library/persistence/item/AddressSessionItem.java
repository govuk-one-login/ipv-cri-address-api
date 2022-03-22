package uk.gov.di.ipv.cri.address.library.persistence.item;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import uk.gov.di.ipv.cri.address.library.helpers.ListOfMapConverter;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@DynamoDbBean
public class AddressSessionItem {
    private UUID sessionId;
    private long expiryDate;
    private String clientId;
    private String state;
    private URI redirectUri;
    private List<CanonicalAddressWithResidency> addresses;
    private UUID authorizationCode;

    public AddressSessionItem() {

        sessionId = UUID.randomUUID();
        addresses = new ArrayList<>();
    }

    @DynamoDbPartitionKey()
    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "authorizationCode-index")
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

    @DynamoDbConvertedBy(ListOfMapConverter.class)
    public List<CanonicalAddressWithResidency> getAddresses() {
        // Handle sessions created before the addresses were added to the session
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        return addresses;
    }

    public void setAddresses(List<CanonicalAddressWithResidency> addresses) {
        this.addresses = addresses;
    }
}
