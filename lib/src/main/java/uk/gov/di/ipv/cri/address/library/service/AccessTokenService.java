package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccessTokenService {
    public static final String CODE = "code";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    private final Long bearAccessTokenTtl;
    private final ConfigurationService configurationService;
    private DataStore<AddressSessionItem> dataStore;

    public AccessTokenService(
            DataStore<AddressSessionItem> dataStore, ConfigurationService configurationService) {
        this.dataStore = dataStore;
        this.bearAccessTokenTtl = configurationService.getBearerAccessTokenTtl();
        this.configurationService = configurationService;
    }

    public AccessTokenService() {
        this.configurationService = new ConfigurationService();
        this.bearAccessTokenTtl = configurationService.getBearerAccessTokenTtl();
        this.dataStore =
                new DataStore<>(
                        configurationService.getAddressSessionTableName(),
                        AddressSessionItem.class,
                        DataStore.getClient());
    }

    public AddressSessionItem getAddressSession(TokenRequest tokenRequest) {
        String authorizationCodeFromRequest =
                ((AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant())
                        .getAuthorizationCode()
                        .getValue();

        return new ListUtil()
                .getValueOrThrow(
                        dataStore.getItemByGsi(
                                getAuthorizationCodeIndex(), authorizationCodeFromRequest));
    }

    public TokenRequest createTokenRequest(String requestBody)
            throws com.nimbusds.oauth2.sdk.ParseException {
        // The URI is not needed/consumed in the resultant TokenRequest
        // therefore any value can be passed here to ensure the parse method
        // successfully materialises a TokenRequest
        URI arbitraryUri = URI.create("https://gds");
        HTTPRequest request = new HTTPRequest(HTTPRequest.Method.POST, arbitraryUri);
        request.setQuery(requestBody);

        boolean invalidTokenRequest =
                request.getQueryParameters()
                        .keySet()
                        .containsAll(Set.of(CODE, CLIENT_ID, REDIRECT_URI, GRANT_TYPE));

        if (!invalidTokenRequest) {
            throw new AccessTokenRequestException(OAuth2Error.INVALID_REQUEST);
        }

        validateTokenRequest(request.getQueryParameters());

        request.setContentType(ContentType.APPLICATION_URLENCODED.getType());
        return TokenRequest.parse(request);
    }

    public TokenResponse createToken(TokenRequest tokenRequest) {
        AccessToken accessToken =
                new BearerAccessToken(bearAccessTokenTtl, tokenRequest.getScope());
        return new AccessTokenResponse(new Tokens(accessToken, null));
    }

    public void writeToken(
            AccessTokenResponse tokenResponse, AddressSessionItem addressSessionItem) {
        addressSessionItem.setAccessToken(
                tokenResponse.getTokens().getBearerAccessToken().toAuthorizationHeader());

        dataStore.update(addressSessionItem);
    }

    private void validateTokenRequest(Map<String, List<String>> queryParameters)
            throws AccessTokenRequestException {

        var listHelper = new ListUtil();

        var authorizationCode =
                listHelper.getValueOrThrow(
                        queryParameters.getOrDefault(CODE, Collections.emptyList()));
        var redirectUri =
                listHelper.getValueOrThrow(
                        queryParameters.getOrDefault(REDIRECT_URI, Collections.emptyList()));
        var grantType =
                listHelper.getValueOrThrow(
                        queryParameters.getOrDefault(GRANT_TYPE, Collections.emptyList()));

        var listUtil = new ListUtil();
        var addressSessionItem =
                listUtil.getValueOrThrow(
                        dataStore.getItemByGsi(getAuthorizationCodeIndex(), authorizationCode));

        if (!grantType.equals(GrantType.AUTHORIZATION_CODE.getValue())) {
            throw new AccessTokenRequestException(OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        }
        if (addressSessionItem == null
                || !authorizationCode.equals(addressSessionItem.getAuthorizationCode())) {
            throw new AccessTokenRequestException(
                    "Cannot retrieve Address session item for the given authorization Code",
                    OAuth2Error.INVALID_GRANT);
        }
        if (!URI.create(redirectUri).equals(addressSessionItem.getRedirectUri())) {
            throw new AccessTokenRequestException(
                    String.format(
                            "Requested redirectUri: %s does not match existing redirectUri: %s",
                            redirectUri, addressSessionItem),
                    OAuth2Error.INVALID_GRANT);
        }
    }

    private DynamoDbIndex<AddressSessionItem> getAuthorizationCodeIndex() {
        return dataStore.getTable().index(AddressSessionItem.AUTHORIZATION_CODE_INDEX);
    }
}
