package uk.gov.di.ipv.cri.address.library.service;

import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

public class CredentialIssuerService {
    private final DataStore<AddressSessionItem> dataStore;

    public CredentialIssuerService() {
        var configurationService = new ConfigurationService();
        dataStore = getDataStore(configurationService);
    }

    public CredentialIssuerService(DataStore<AddressSessionItem> dataStore) {
        this.dataStore = dataStore;
    }

    public AddressSessionItem getAddressSessionItem(String accessToken)
            throws DynamoDbException, CredentialRequestException {
        var index = dataStore.getTable().index(AddressSessionItem.ACCESS_TOKEN_INDEX);
        try {
            return new ListUtil().getValueOrThrow(dataStore.getItemByGsi(index, accessToken));
        } catch (IllegalArgumentException ie) {
            throw new CredentialRequestException(
                    ErrorResponse.MISSING_ADDRESS_SESSION_ITEM.getMessage(), ie);
        }
    }

    private DataStore<AddressSessionItem> getDataStore(ConfigurationService configurationService) {
        return new DataStore<>(
                configurationService.getAddressSessionTableName(),
                AddressSessionItem.class,
                DataStore.getClient());
    }
}
