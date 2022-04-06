package uk.gov.di.ipv.cri.address.library.service;

import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.util.List;
import java.util.UUID;

public class CredentialIssuerService {
    private final DataStore<AddressSessionItem> dataStore;

    public CredentialIssuerService() {
        var configurationService = new ConfigurationService();
        dataStore = getDataStore(configurationService);
    }

    public CredentialIssuerService(DataStore<AddressSessionItem> dataStore) {
        this.dataStore = dataStore;
    }

    public UUID getSessionId(String accessToken)
            throws DynamoDbException, CredentialRequestException {
        var addressSessionTable = dataStore.getTable();
        var index = addressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX);
        try {
            var listHelper = new ListUtil();
            var addressSessionItem =
                    listHelper.getValueOrThrow(dataStore.getItemByGsi(index, accessToken));
            return addressSessionItem.getSessionId();
        } catch (IllegalArgumentException ie) {
            throw new CredentialRequestException(
                    ErrorResponse.MISSING_ADDRESS_SESSION_ITEM.getMessage(), ie);
        }
    }

    public List<CanonicalAddressWithResidency> getAddresses(UUID sessionId)
            throws CredentialRequestException {
        var addressSessionItem = dataStore.getItem(sessionId.toString());
        if (addressSessionItem == null) {
            throw new CredentialRequestException(ErrorResponse.MISSING_ADDRESS_SESSION_ITEM);
        }
        return addressSessionItem.getAddresses();
    }

    private DataStore<AddressSessionItem> getDataStore(ConfigurationService configurationService) {
        return new DataStore<>(
                configurationService.getAddressSessionTableName(),
                AddressSessionItem.class,
                DataStore.getClient());
    }
}
