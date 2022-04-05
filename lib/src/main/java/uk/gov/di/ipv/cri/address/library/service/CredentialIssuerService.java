package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CredentialIssuerService {
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private final DataStore<AddressSessionItem> dataStore;

    public CredentialIssuerService() {
        var configurationService = new ConfigurationService();
        dataStore = getDataStore(configurationService);
    }

    public CredentialIssuerService(DataStore<AddressSessionItem> dataStore) {
        this.dataStore = dataStore;
    }

    public UUID getSessionId(APIGatewayProxyRequestEvent input)
            throws CredentialRequestException, DynamoDbException, ParseException {

        var accessToken = validateInputHeaderBearerToken(input.getHeaders());

        var addressSessionTable = dataStore.getTable();
        var index = addressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX);

        var listHelper = new ListUtil();
        var addressSessionItem =
                listHelper.getValueOrThrow(
                        dataStore.getItemByGsi(index, accessToken.toAuthorizationHeader()));

        return addressSessionItem.getSessionId();
    }

    public List<CanonicalAddressWithResidency> getAddresses(UUID sessionId)
            throws CredentialRequestException {
        var addressSessionItem = dataStore.getItem(sessionId.toString());
        if (addressSessionItem == null) {
            throw new CredentialRequestException(ErrorResponse.MISSING_ADDRESS_SESSION_ITEM);
        }
        return addressSessionItem.getAddresses();
    }

    private AccessToken validateInputHeaderBearerToken(Map<String, String> headers)
            throws CredentialRequestException, ParseException {
        var token =
                Optional.ofNullable(headers).stream()
                        .flatMap(x -> x.entrySet().stream())
                        .filter(e -> AUTHORIZATION_HEADER_KEY.equalsIgnoreCase(e.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new CredentialRequestException(
                                                ErrorResponse.MISSING_AUTHORIZATION_HEADER));

        return AccessToken.parse(token, AccessTokenType.BEARER);
    }

    private DataStore<AddressSessionItem> getDataStore(ConfigurationService configurationService) {
        return new DataStore<>(
                configurationService.getAddressSessionTableName(),
                AddressSessionItem.class,
                DataStore.getClient());
    }
}
