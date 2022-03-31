package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.nio.charset.Charset;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CredentialIssuerService {
    public static final String AUTHORIZATION = "Authorization";
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
        var queryParams = queryParams(input.getBody());
        if (!queryParams.containsKey(SUB)) {
            throw new CredentialRequestException(ErrorResponse.INVALID_REQUEST_PARAM);
        }
        var subject = queryParams.get(SUB);
        System.out.println("The input header is: ");
        System.out.println(input.getHeaders());
        var accessToken = getAccessToken(input.getHeaders());
        System.out.println("This access token is: " + accessToken);

        AddressSessionItem addressSessionItem =
                addressSessionService.getItemByGSIIndex(
                        accessToken, AddressSessionItem.TOKEN_INDEX);

        return addressSessionItem.getSessionId();
    }

    public String getAccessToken(Map<String, String> headers) throws CredentialRequestException {
        return Optional.ofNullable(headers).stream()
                .flatMap(x -> x.entrySet().stream())
                .filter(e -> AUTHORIZATION.equalsIgnoreCase(e.getKey()))
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
