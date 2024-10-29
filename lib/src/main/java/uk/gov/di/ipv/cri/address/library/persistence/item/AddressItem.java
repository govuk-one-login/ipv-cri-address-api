package uk.gov.di.ipv.cri.address.library.persistence.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@DynamoDbBean
public class AddressItem {
    private UUID sessionId;
    private long expiryDate;
    private List<CanonicalAddress> addresses = new ArrayList<>();

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

    public List<CanonicalAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<CanonicalAddress> addresses) {
        this.addresses = Objects.requireNonNullElseGet(addresses, ArrayList::new);
    }
}
