package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenValidationException;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
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
    public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    public static final String CLIENT_ASSERTION = "client_assertion";
    private final DataStore<AddressSessionItem> dataStore;
    private final Long bearAccessTokenTtl;
    private final ConfigurationService configurationService;
    private final JWTVerifier jwtVerifier;

    public AccessTokenService(
            DataStore<AddressSessionItem> dataStore,
            Long bearerAccessTokenTtl,
            ConfigurationService configurationService,
            JWTVerifier jwtVerifier) {
        this.dataStore = dataStore;
        this.bearAccessTokenTtl = bearerAccessTokenTtl;
        this.configurationService = new ConfigurationService();
        this.jwtVerifier = jwtVerifier;
    }

    public AccessTokenService() {
        var configurationService = new ConfigurationService();
        dataStore = getDataStore(configurationService);
        this.bearAccessTokenTtl = configurationService.getBearerAccessTokenTtl();
        this.configurationService = new ConfigurationService();
        this.jwtVerifier = new JWTVerifier();
    }

    public AddressSessionItem getAddressSession(TokenRequest tokenRequest) {
        String authorizationCodeFromRequest =
                ((AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant())
                        .getAuthorizationCode()
                        .getValue();

        return getItemByAuthorizationCode(authorizationCodeFromRequest);
    }

    public AccessTokenResponse createToken(TokenRequest tokenRequest) {
        AccessToken accessToken =
                new BearerAccessToken(bearAccessTokenTtl, tokenRequest.getScope());
        return new AccessTokenResponse(new Tokens(accessToken, null)).toSuccessResponse();
    }

    public void writeToken(
            AccessTokenResponse tokenResponse, AddressSessionItem addressSessionItem) {
        addressSessionItem.setAccessToken(
                tokenResponse.getTokens().getBearerAccessToken().toAuthorizationHeader());

        dataStore.update(addressSessionItem);
    }

    public TokenRequest createTokenRequest(String requestBody)
            throws AccessTokenValidationException {
        try {
            URI arbitraryUri = URI.create("https://gds");
            HTTPRequest request = new HTTPRequest(HTTPRequest.Method.POST, arbitraryUri);
            request.setQuery(requestBody);
            request.setContentType(ContentType.APPLICATION_URLENCODED.getType());

            if (!request.getQueryParameters()
                    .keySet()
                    .containsAll(
                            Set.of(CODE, CLIENT_ID, CLIENT_ASSERTION_TYPE, CLIENT_ASSERTION))) {
                throw new AccessTokenValidationException("missing request parameters");
            }

            return TokenRequest.parse(request);
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            throw new AccessTokenValidationException(e);
        }
    }

    public TokenRequest validateTokenRequest(TokenRequest tokenRequest)
            throws AccessTokenValidationException {
        try {

            ClientAuthentication clientAuthentication = tokenRequest.getClientAuthentication();
            PrivateKeyJWT privateKeyJWT = (PrivateKeyJWT) clientAuthentication;
            AuthorizationCodeGrant authorizationGrant =
                    (AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant();

            validateTokenRequestToRecord(
                    privateKeyJWT, authorizationGrant, tokenRequest.getClientID());

            Map<String, String> clientAuthenticationConfig =
                    getClientAuthenticationConfig(tokenRequest.getClientID().getValue());
            SignedJWT signedJWT = privateKeyJWT.getClientAssertion();

            jwtVerifier.verifyJWTHeader(clientAuthenticationConfig, signedJWT);
            jwtVerifier.verifyJWTClaimsSet(clientAuthenticationConfig, signedJWT);
            jwtVerifier.verifyJWTSignature(clientAuthenticationConfig, signedJWT);
            return tokenRequest;
        } catch (SessionValidationException | ClientConfigurationException e) {
            throw new AccessTokenValidationException(e);
        }
    }

    private Map<String, String> getClientAuthenticationConfig(String clientId)
            throws SessionValidationException {
        String path = String.format("/clients/%s/jwtAuthentication", clientId);
        Map<String, String> clientConfig = configurationService.getParametersForPath(path);
        if (clientConfig == null || clientConfig.isEmpty()) {
            throw new SessionValidationException(
                    String.format("no configuration for client id '%s'", clientId));
        }
        return clientConfig;
    }

    private void validateTokenRequestToRecord(
            PrivateKeyJWT privateKeyJWT,
            AuthorizationCodeGrant authorizationGrant,
            ClientID clientID)
            throws AccessTokenValidationException {

        AuthorizationCode authorizationCode = authorizationGrant.getAuthorizationCode();
        AddressSessionItem addressSessionItem =
                getAddressSessionItemByAuthorizationCodeIndex(authorizationCode.getValue());

        if (addressSessionItem == null
                || !authorizationCode.equals(addressSessionItem.getAuthorizationCode())) {
            throw new AccessTokenValidationException(
                    "Cannot for the Address session item for the given authorization Code");
        }

        Issuer jwtIssuer = privateKeyJWT.getJWTAuthenticationClaimsSet().getIssuer();
        Subject subject = privateKeyJWT.getJWTAuthenticationClaimsSet().getSubject();
        boolean issuerMatchesSubject = jwtIssuer.getValue().equals(subject.getValue());
        boolean issuerMatchesTokenRequestClientID =
                jwtIssuer.getValue().equals(clientID.getValue());
        boolean issuerMatchesClientIDOnRecord =
                jwtIssuer.getValue().equals(addressSessionItem.getClientId());

        if (!(issuerMatchesSubject
                && issuerMatchesTokenRequestClientID
                && issuerMatchesClientIDOnRecord)) {
            throw new AccessTokenValidationException(
                    "issuer, sub, request client id, saved client id do not match ");
        }
    }

    public AddressSessionItem getAddressSessionItemByAuthorizationCodeIndex(final String value) {
        DynamoDbTable<AddressSessionItem> addressSessionTable = dataStore.getTable();
        DynamoDbIndex<AddressSessionItem> index =
                addressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX);

        AttributeValue attVal = AttributeValue.builder().s(value).build();

        QueryConditional queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        SdkIterable<Page<AddressSessionItem>> items =
                index.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build());

        List<AddressSessionItem> item =
                items.stream().map(Page::items).findFirst().orElseGet(Collections::emptyList);

        return item.stream().findFirst().orElseThrow();
    }

    private AddressSessionItem getItemByAuthorizationCode(String authorizationCodeFromRequest) {
        var addressSessionTable = dataStore.getTable();
        var index = addressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX);
        var listHelper = new ListUtil();

        return listHelper.getValueOrThrow(
                dataStore.getItemByGsi(index, authorizationCodeFromRequest));
    }

    private DataStore<AddressSessionItem> getDataStore(ConfigurationService configurationService) {
        return new DataStore<>(
                configurationService.getAddressSessionTableName(),
                AddressSessionItem.class,
                DataStore.getClient());
    }
}
