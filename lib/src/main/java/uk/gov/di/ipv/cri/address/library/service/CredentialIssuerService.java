package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CredentialIssuerService {
    public static final String ACCESS_TOKEN = "access_token";
    public static final String SUB = "sub";
    private final AddressSessionService addressSessionService;

    public CredentialIssuerService() {
        var configurationService = new ConfigurationService();
        var dataStore = getDataStore(configurationService);
        this.addressSessionService =
                new AddressSessionService(dataStore, configurationService, Clock.systemUTC());
    }

    public CredentialIssuerService(AddressSessionService addressSessionService) {
        this.addressSessionService = addressSessionService;
    }

    public UUID getSessionId(APIGatewayProxyRequestEvent input) throws CredentialRequestException {
        var queryParams = queryParams(input);
        var listHelper = new ListUtil();
        var accessToken =
                listHelper.getValueOrThrow(
                        queryParams.getOrDefault(ACCESS_TOKEN, Collections.emptyList()));

        AddressSessionItem addressSessionItem =
                addressSessionService.getItemByGSIIndex(
                        accessToken, AddressSessionItem.TOKEN_INDEX);

        return addressSessionItem.getSessionId();
    }

    private Map<String, List<String>> queryParams(APIGatewayProxyRequestEvent input) {
        return Optional.ofNullable(
                        input.getQueryStringParameters().entrySet().stream()
                                .collect(
                                        Collectors.toMap(
                                                Map.Entry::getKey, e -> List.of(e.getValue()))))
                .orElseGet(Collections::emptyMap);
    }

    public List<CanonicalAddressWithResidency> getAddresses(UUID sessionId)
            throws CredentialRequestException {
        AddressSessionItem addressSessionItem =
                addressSessionService.getSession(sessionId.toString());
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
