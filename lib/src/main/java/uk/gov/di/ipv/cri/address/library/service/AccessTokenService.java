package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.JWTID;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenValidationException;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AccessTokenService {
    public static final String CODE = "code";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    public static final String CLIENT_ASSERTION = "client_assertion";
    public static final String AUTHORISATION_CODE = "authorization_code";
    private final DataStore<AddressSessionItem> dataStore;
    private final Long bearAccessTokenTtl;
    private final ConfigurationService configurationService;
    private final JWTVerifier jwtVerifier;
    private ListUtil listHelper = new ListUtil();

    public AccessTokenService(
            DataStore<AddressSessionItem> dataStore,
            Long bearerAccessTokenTtl,
            ConfigurationService configurationService,
            JWTVerifier jwtVerifier) {
        this.dataStore = dataStore;
        this.bearAccessTokenTtl = bearerAccessTokenTtl;
        this.configurationService = configurationService;
        this.jwtVerifier = jwtVerifier;
        this.listHelper = new ListUtil();
    }

    @ExcludeFromGeneratedCoverageReport
    public AccessTokenService() {
        var configurationService = new ConfigurationService();
        dataStore = getDataStore(configurationService);
        this.bearAccessTokenTtl = configurationService.getBearerAccessTokenTtl();
        this.configurationService = new ConfigurationService();
        this.jwtVerifier = new JWTVerifier();
    }

    public UUID getAddressSessionId(TokenRequest tokenRequest) {
        String authorizationCodeFromRequest =
                ((AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant())
                        .getAuthorizationCode()
                        .getValue();

        return getItemByAuthorizationCode(authorizationCodeFromRequest).getSessionId();
    }

    public AddressSessionItem getAddressSessionItem(UUID sessionId) {
        return dataStore.getItem(sessionId.toString());
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
                            Set.of(
                                    CODE,
                                    CLIENT_ID,
                                    CLIENT_ASSERTION_TYPE,
                                    CLIENT_ASSERTION,
                                    GRANT_TYPE))) {
                throw new AccessTokenValidationException(OAuth2Error.INVALID_REQUEST.getCode());
            }

            request.getQueryParameters().values().stream()
                    .filter(param -> param.contains(AUTHORISATION_CODE))
                    .findFirst()
                    .orElseThrow(
                            () ->
                                    new AccessTokenValidationException(
                                            OAuth2Error.UNSUPPORTED_GRANT_TYPE_CODE));

            return TokenRequest.parse(request);
        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            throw new AccessTokenValidationException(e);
        }
    }

    public TokenRequest validateTokenRequest(
            TokenRequest tokenRequest, AddressSessionItem addressSessionItem)
            throws AccessTokenValidationException {
        try {

            ClientAuthentication clientAuthentication = tokenRequest.getClientAuthentication();
            PrivateKeyJWT privateKeyJWT = (PrivateKeyJWT) clientAuthentication;
            AuthorizationCodeGrant authorizationGrant =
                    (AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant();

            validateTokenRequestToRecord(
                    privateKeyJWT,
                    authorizationGrant,
                    tokenRequest.getClientAuthentication().getClientID(),
                    addressSessionItem);

            Map<String, String> clientAuthenticationConfig =
                    getClientAuthenticationConfig(
                            tokenRequest.getClientAuthentication().getClientID().getValue());
            SignedJWT signedJWT = privateKeyJWT.getClientAssertion();

            jwtVerifier.verifyJWT(clientAuthenticationConfig, signedJWT);
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
            ClientID clientID,
            AddressSessionItem addressSessionItem)
            throws AccessTokenValidationException {

        AuthorizationCode authorizationCode = authorizationGrant.getAuthorizationCode();

        if (!authorizationCode.getValue().equals(addressSessionItem.getAuthorizationCode())) {
            throw new AccessTokenValidationException(
                    "Authorisation code does not match with authorization Code for Address Session Item");
        }

        Issuer jwtIssuer = privateKeyJWT.getJWTAuthenticationClaimsSet().getIssuer();
        Subject subject = privateKeyJWT.getJWTAuthenticationClaimsSet().getSubject();
        List<Audience> audience = privateKeyJWT.getJWTAuthenticationClaimsSet().getAudience();
        JWTID jwtid = privateKeyJWT.getJWTAuthenticationClaimsSet().getJWTID();
        boolean isAudiencePresent = !audience.isEmpty();
        boolean isJWTIdPresent = jwtid != null;
        boolean issuerMatchesSubject = jwtIssuer.getValue().equals(subject.getValue());
        boolean issuerMatchesTokenRequestClientID =
                jwtIssuer.getValue().equals(clientID.getValue());
        boolean issuerMatchesClientIDOnRecord =
                jwtIssuer.getValue().equals(addressSessionItem.getClientId());

        if (!(issuerMatchesSubject
                && issuerMatchesTokenRequestClientID
                && issuerMatchesClientIDOnRecord
                && isAudiencePresent
                && isJWTIdPresent)) {
            throw new AccessTokenValidationException(
                    "issuer, sub, audience or jti are missing (or) request client id and saved client id do not match");
        }
    }

    private AddressSessionItem getItemByAuthorizationCode(String authorizationCodeFromRequest) {
        var addressSessionTable = dataStore.getTable();
        var index = addressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX);
        return listHelper.getOneItemOrThrowError(
                dataStore.getItemByGsi(index, authorizationCodeFromRequest));
    }

    private DataStore<AddressSessionItem> getDataStore(ConfigurationService configurationService) {
        return new DataStore<>(
                configurationService.getAddressSessionTableName(),
                AddressSessionItem.class,
                DataStore.getClient());
    }
}
