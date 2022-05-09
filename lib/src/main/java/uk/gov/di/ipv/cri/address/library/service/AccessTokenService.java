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
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenRequestException;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenValidationException;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.persistence.item.SessionItem;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccessTokenService {
    public static final String CODE = "code";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    public static final String CLIENT_ASSERTION = "client_assertion";
    public static final String AUTHORISATION_CODE = "authorization_code";
    public static final String REDIRECT_URI = "redirect_uri";
    private final ConfigurationService configurationService;
    private final JWTVerifier jwtVerifier;

    public AccessTokenService(ConfigurationService configurationService, JWTVerifier jwtVerifier) {
        this.configurationService = configurationService;
        this.jwtVerifier = jwtVerifier;
    }

    @ExcludeFromGeneratedCoverageReport
    public AccessTokenService() {
        this(new ConfigurationService(), new JWTVerifier());
    }

    public String getAuthorizationCode(TokenRequest tokenRequest) {
        return ((AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant())
                .getAuthorizationCode()
                .getValue();
    }

    public AccessTokenResponse createToken(TokenRequest tokenRequest) {
        AccessToken accessToken =
                new BearerAccessToken(
                        configurationService.getBearerAccessTokenTtl(), tokenRequest.getScope());
        return new AccessTokenResponse(new Tokens(accessToken, null)).toSuccessResponse();
    }

    public SessionItem updateSessionAccessToken(
            SessionItem sessionItem, AccessTokenResponse tokenResponse) {
        sessionItem.setAccessToken(
                tokenResponse.getTokens().getBearerAccessToken().toAuthorizationHeader());
        return sessionItem;
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

    public TokenRequest validateTokenRequest(TokenRequest tokenRequest, SessionItem sessionItem)
            throws AccessTokenValidationException {
        try {

            ClientAuthentication clientAuthentication = tokenRequest.getClientAuthentication();
            PrivateKeyJWT privateKeyJWT = (PrivateKeyJWT) clientAuthentication;
            AuthorizationCodeGrant authorizationGrant =
                    (AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant();

            ClientID clientID = tokenRequest.getClientAuthentication().getClientID();
            validateTokenRequestToRecord(privateKeyJWT, authorizationGrant, clientID, sessionItem);

            Map<String, String> clientAuthenticationConfig =
                    getClientAuthenticationConfig(clientID.getValue());
            SignedJWT signedJWT = privateKeyJWT.getClientAssertion();

            jwtVerifier.verifyAccessTokenJWT(clientAuthenticationConfig, signedJWT, clientID);
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
            SessionItem sessionItem)
            throws AccessTokenValidationException, AccessTokenRequestException,
                    SessionValidationException {

        AuthorizationCode authorizationCode = authorizationGrant.getAuthorizationCode();

        if (!authorizationCode.getValue().equals(sessionItem.getAuthorizationCode())) {
            throw new AccessTokenRequestException(
                    "Authorisation code does not match with authorization Code for Address Session Item",
                    OAuth2Error.INVALID_GRANT);
        }
        Map<String, String> clientAuthenticationConfig =
                getClientAuthenticationConfig(clientID.getValue());

        verifyRequestUri(sessionItem.getRedirectUri(), clientAuthenticationConfig);
        verifyPrivateKeyJWTAttributes(privateKeyJWT, clientID, sessionItem);
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
            PrivateKeyJWT privateKeyJWT, ClientID clientID, SessionItem sessionItem)
            throws AccessTokenValidationException {

        Issuer jwtIssuer = privateKeyJWT.getJWTAuthenticationClaimsSet().getIssuer();
        Subject subject = privateKeyJWT.getJWTAuthenticationClaimsSet().getSubject();
        List<Audience> audience = privateKeyJWT.getJWTAuthenticationClaimsSet().getAudience();
        JWTID jwtid = privateKeyJWT.getJWTAuthenticationClaimsSet().getJWTID();

        verifyIfAudiencePresent(audience);
        verifyIfJWTIdPresent(jwtid);
        verifyIfIssuerMatchesSubject(jwtIssuer, subject);
        verifyIfIssuerMatchesTokenRequestClientID(jwtIssuer, clientID);
        verifyIssuerMatchesClientIDOnRecord(jwtIssuer, sessionItem);
    }

    private void verifyIssuerMatchesClientIDOnRecord(Issuer jwtIssuer, SessionItem sessionItem)
            throws AccessTokenValidationException {
        if (!jwtIssuer.getValue().equals(sessionItem.getClientId())) {
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
}
