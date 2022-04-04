package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CredentialIssuerService {
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    public static final String SUB = "sub";
    private final DataStore<AddressSessionItem> dataStore;

    public CredentialIssuerService() {
        var configurationService = new ConfigurationService();
        dataStore = getDataStore(configurationService);
    }

    public CredentialIssuerService(DataStore<AddressSessionItem> dataStore) {
        this.dataStore = dataStore;
    }

    public UUID getSessionId(APIGatewayProxyRequestEvent input)
            throws CredentialRequestException, DynamoDbException {
        var queryParams = queryParams(input.getBody());
        if (!queryParams.containsKey(SUB)) {
            throw new CredentialRequestException(ErrorResponse.INVALID_REQUEST_PARAM);
        }
        var accessToken = getAccessToken(input.getHeaders());
        var addressSessionTable = dataStore.getTable();
        var index = addressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX);

        var listHelper = new ListUtil();
        var addressSessionItem =
                listHelper.getValueOrThrow(dataStore.getItemByGsi(index, accessToken));

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

    private String getAccessToken(Map<String, String> headers) throws CredentialRequestException {
        return Optional.ofNullable(headers).stream()
                .flatMap(x -> x.entrySet().stream())
                .filter(e -> AUTHORIZATION_HEADER_KEY.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(
                        () ->
                                new CredentialRequestException(
                                        ErrorResponse.MISSING_AUTHORIZATION_HEADER));
    }

    private Map<String, String> queryParams(String body) {
        return URLEncodedUtils.parse(body, Charset.defaultCharset()).stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    }

    private DataStore<AddressSessionItem> getDataStore(ConfigurationService configurationService) {
        return new DataStore<>(
                configurationService.getAddressSessionTableName(),
                AddressSessionItem.class,
                DataStore.getClient());
    }
}
