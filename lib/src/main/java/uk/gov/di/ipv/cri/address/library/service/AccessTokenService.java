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
import uk.gov.di.ipv.cri.address.library.constants.RequiredClaims;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenRequestException;
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
    public static final String REDIRECT_URI = "redirect_uri";
    private final DataStore<AddressSessionItem> dataStore;
    private final ConfigurationService configurationService;
    private final JWTVerifier jwtVerifier;

    public AccessTokenService(
            DataStore<AddressSessionItem> dataStore,
            ConfigurationService configurationService,
            JWTVerifier jwtVerifier) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.jwtVerifier = jwtVerifier;
    }

    @ExcludeFromGeneratedCoverageReport
    public AccessTokenService() {
        this.configurationService = new ConfigurationService();
        dataStore = getDataStore(this.configurationService);
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
                new BearerAccessToken(
                        configurationService.getBearerAccessTokenTtl(), tokenRequest.getScope());
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
                                    CLIENT_ASSERTION_TYPE,
                                    CLIENT_ASSERTION,
                                    REDIRECT_URI,
                                    GRANT_TYPE))) {
                throw new AccessTokenValidationException(OAuth2Error.INVALID_REQUEST.getCode());
            }

            if (request.getQueryParameters().values().stream()
                    .noneMatch(param -> param.contains(AUTHORISATION_CODE))) {
                throw new AccessTokenValidationException(OAuth2Error.UNSUPPORTED_GRANT_TYPE_CODE);
            }

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

            jwtVerifier.verifyJWT(
                    clientAuthenticationConfig,
                    signedJWT,
                    List.of(RequiredClaims.EXP.value, RequiredClaims.SUB.value));
            return tokenRequest;
        } catch (SessionValidationException
                | ClientConfigurationException
                | AccessTokenRequestException e) {
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
            throws AccessTokenValidationException, AccessTokenRequestException,
                    SessionValidationException {

        AuthorizationCode authorizationCode = authorizationGrant.getAuthorizationCode();

        if (!authorizationCode.getValue().equals(addressSessionItem.getAuthorizationCode())) {
            throw new AccessTokenRequestException(
                    "Authorisation code does not match with authorization Code for Address Session Item",
                    OAuth2Error.INVALID_GRANT);
        }
        Map<String, String> clientAuthenticationConfig =
                getClientAuthenticationConfig(clientID.getValue());

        verifyRequestUri(addressSessionItem.getRedirectUri(), clientAuthenticationConfig);
        verifyPrivateKeyJWTAttributes(privateKeyJWT, clientID, addressSessionItem);
    }

    private void verifyRequestUri(URI requestRedirectUri, Map<String, String> clientConfig)
            throws AccessTokenValidationException {
        URI configRedirectUri = URI.create(clientConfig.get("redirectUri"));
        if (requestRedirectUri == null || !requestRedirectUri.equals(configRedirectUri)) {
            throw new AccessTokenValidationException(
                    "redirect uri "
                            + requestRedirectUri
                            + " does not match configuration uri "
                            + configRedirectUri);
        }
    }

    private void verifyPrivateKeyJWTAttributes(
            PrivateKeyJWT privateKeyJWT, ClientID clientID, AddressSessionItem addressSessionItem)
            throws AccessTokenValidationException {

        Issuer jwtIssuer = privateKeyJWT.getJWTAuthenticationClaimsSet().getIssuer();
        Subject subject = privateKeyJWT.getJWTAuthenticationClaimsSet().getSubject();
        List<Audience> audience = privateKeyJWT.getJWTAuthenticationClaimsSet().getAudience();
        JWTID jwtid = privateKeyJWT.getJWTAuthenticationClaimsSet().getJWTID();

        verifyIfAudiencePresent(audience);
        verifyIfJWTIdPresent(jwtid);
        verifyIfIssuerMatchesSubject(jwtIssuer, subject);
        verifyIfIssuerMatchesTokenRequestClientID(jwtIssuer, clientID);
        verifyIssuerMatchesClientIDOnRecord(jwtIssuer, addressSessionItem);
    }

    private void verifyIssuerMatchesClientIDOnRecord(
            Issuer jwtIssuer, AddressSessionItem addressSessionItem)
            throws AccessTokenValidationException {
        if (!jwtIssuer.getValue().equals(addressSessionItem.getClientId())) {
            throwValidationException("request client id and saved client id do not match");
        }
    }

    private void verifyIfIssuerMatchesTokenRequestClientID(Issuer jwtIssuer, ClientID clientID)
            throws AccessTokenValidationException {
        if (!jwtIssuer.getValue().equals(clientID.getValue())) {
            throwValidationException("issuer does not match clientID");
        }
    }

    private void verifyIfIssuerMatchesSubject(Issuer jwtIssuer, Subject subject)
            throws AccessTokenValidationException {
        if (!jwtIssuer.getValue().equals(subject.getValue())) {
            throwValidationException("issuer does not match subject");
        }
    }

    private void verifyIfJWTIdPresent(JWTID jwtid) throws AccessTokenValidationException {
        if (jwtid == null) {
            throwValidationException("jti is missing");
        }
    }

    private void verifyIfAudiencePresent(List<Audience> audience)
            throws AccessTokenValidationException {
        if (audience.isEmpty()) {
            throwValidationException("audience is missing");
        }
    }

    private void throwValidationException(String errorMessage)
            throws AccessTokenValidationException {
        throw new AccessTokenValidationException(errorMessage);
    }

    private AddressSessionItem getItemByAuthorizationCode(String authorizationCodeFromRequest) {
        var addressSessionTable = dataStore.getTable();
        var index = addressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX);
        var listHelper = new ListUtil();
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
