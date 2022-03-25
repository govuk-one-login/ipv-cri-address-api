package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
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
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenRequestException;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;
import uk.gov.di.ipv.cri.address.library.validation.ValidationResult;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AddressSessionService {

    public static final String CODE = "code";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    private final DataStore<AddressSessionItem> dataStore;
    private final Clock clock;
    private final ConfigurationService configurationService;

    @ExcludeFromGeneratedCoverageReport
    public AddressSessionService() {
        this.configurationService = new ConfigurationService();
        this.dataStore =
                new DataStore<>(
                        configurationService.getAddressSessionTableName(),
                        AddressSessionItem.class,
                        DataStore.getClient());
        clock = Clock.systemUTC();
    }

    public AddressSessionService(
            DataStore<AddressSessionItem> dataStore,
            ConfigurationService configurationService,
            Clock clock) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.clock = clock;
    }

    public ValidationResult<ErrorObject> validateTokenRequest(TokenRequest tokenRequest) {
        if (!tokenRequest.getAuthorizationGrant().getType().equals(GrantType.AUTHORIZATION_CODE)) {
            return new ValidationResult<>(false, OAuth2Error.UNSUPPORTED_GRANT_TYPE);
        }

        return ValidationResult.createValidResult();
    }

    public UUID createAndSaveAddressSession(SessionRequest sessionRequest) {

        AddressSessionItem addressSessionItem = new AddressSessionItem();
        addressSessionItem.setExpiryDate(
                clock.instant()
                        .plus(configurationService.getAddressSessionTtl(), ChronoUnit.SECONDS)
                        .getEpochSecond());
        addressSessionItem.setState(sessionRequest.getState());
        addressSessionItem.setClientId(sessionRequest.getClientId());
        addressSessionItem.setRedirectUri(sessionRequest.getRedirectUri());
        dataStore.create(addressSessionItem);
        return addressSessionItem.getSessionId();
    }

    public SessionRequest validateSessionRequest(String requestBody)
            throws SessionValidationException, ClientConfigurationException {

        SessionRequest sessionRequest = parseSessionRequest(requestBody);
        Map<String, String> clientAuthenticationConfig =
                getClientAuthenticationConfig(sessionRequest.getClientId());

        verifyRequestUri(sessionRequest, clientAuthenticationConfig);

        SignedJWT signedJWT = parseRequestJWT(sessionRequest);
        verifyJWTHeader(clientAuthenticationConfig, signedJWT);
        verifyJWTClaimsSet(clientAuthenticationConfig, signedJWT);
        verifyJWTSignature(clientAuthenticationConfig, signedJWT);

        return sessionRequest;
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

    private void validateTokenRequest(Map<String, List<String>> queryParameters)
            throws AccessTokenRequestException {

        var authorizationCode =
                getValueOrThrow(queryParameters.getOrDefault(CODE, Collections.emptyList()));
        var redirectUri =
                getValueOrThrow(
                        queryParameters.getOrDefault(REDIRECT_URI, Collections.emptyList()));
        var grantType =
                getValueOrThrow(queryParameters.getOrDefault(GRANT_TYPE, Collections.emptyList()));

        var addressSessionItem = getAddressSessionItemByValue(authorizationCode);
        throw new AccessTokenRequestException(
                "Cannot for the Address session item for the given authorization Code",
                OAuth2Error.INVALID_GRANT);
    }

    private SessionRequest parseSessionRequest(String requestBody)
            throws SessionValidationException {
        try {
            return new ObjectMapper().readValue(requestBody, SessionRequest.class);
        } catch (JsonProcessingException e) {
            throw new SessionValidationException("could not parse request body", e);
        }
    }

    private SignedJWT parseRequestJWT(SessionRequest sessionRequest)
            throws SessionValidationException {
        try {
            return SignedJWT.parse(sessionRequest.getRequestJWT());
        } catch (ParseException e) {
            throw new SessionValidationException("Could not parse request JWT", e);
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

    private void verifyRequestUri(SessionRequest sessionRequest, Map<String, String> clientConfig)
            throws SessionValidationException {
        URI configRedirectUri = URI.create(clientConfig.get("redirectUri"));
        URI requestRedirectUri = sessionRequest.getRedirectUri();
        if (requestRedirectUri == null || !requestRedirectUri.equals(configRedirectUri)) {
            throw new SessionValidationException(
                    "redirect uri "
                            + requestRedirectUri
                            + " does not match configuration uri "
                            + configRedirectUri);
        }
    }

    private void verifyJWTHeader(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT)
            throws SessionValidationException {
        JWSAlgorithm configuredAlgorithm =
                JWSAlgorithm.parse(clientAuthenticationConfig.get("authenticationAlg"));
        JWSAlgorithm jwtAlgorithm = signedJWT.getHeader().getAlgorithm();
        if (jwtAlgorithm != configuredAlgorithm) {
            throw new SessionValidationException(
                    String.format(
                            "jwt signing algorithm %s does not match signing algorithm configured for client: %s",
                            jwtAlgorithm, configuredAlgorithm));
        }
    }

    private void verifyJWTSignature(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT)
            throws SessionValidationException, ClientConfigurationException {
        String publicCertificateToVerify =
                clientAuthenticationConfig.get("publicCertificateToVerify");
        try {
            PublicKey pubicKeyFromConfig = getPubicKeyFromConfig(publicCertificateToVerify);

            if (!validSignature(signedJWT, pubicKeyFromConfig)) {
                throw new SessionValidationException("JWT signature verification failed");
            }
        } catch (JOSEException e) {
            throw new SessionValidationException("JWT signature verification failed", e);
        } catch (CertificateException e) {
            throw new ClientConfigurationException(e);
        }
    }

    private void verifyJWTClaimsSet(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT)
            throws SessionValidationException {
        DefaultJWTClaimsVerifier<?> verifier =
                new DefaultJWTClaimsVerifier<>(
                        new JWTClaimsSet.Builder()
                                .issuer(clientAuthenticationConfig.get("issuer"))
                                .build(),
                        new HashSet<>(Arrays.asList("exp", "nbf")));

        try {
            verifier.verify(signedJWT.getJWTClaimsSet(), null);
        } catch (BadJWTException | ParseException e) {
            throw new SessionValidationException("could not parse JWT", e);
        }
    }

    private PublicKey getPubicKeyFromConfig(String base64) throws CertificateException {
        byte[] binaryCertificate = Base64.getDecoder().decode(base64);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        Certificate certificate =
                factory.generateCertificate(new ByteArrayInputStream(binaryCertificate));
        return certificate.getPublicKey();
    }

    private boolean validSignature(SignedJWT signedJWT, PublicKey clientPublicKey)
            throws JOSEException, ClientConfigurationException {
        if (clientPublicKey instanceof RSAPublicKey) {
            RSASSAVerifier rsassaVerifier = new RSASSAVerifier((RSAPublicKey) clientPublicKey);
            return signedJWT.verify(rsassaVerifier);
        } else if (clientPublicKey instanceof ECPublicKey) {
            ECDSAVerifier ecdsaVerifier = new ECDSAVerifier((ECPublicKey) clientPublicKey);
            return signedJWT.verify(ecdsaVerifier);
        } else {
            throw new ClientConfigurationException(
                    new IllegalStateException("unknown public JWT signing key"));
        }
    }

    public List<CanonicalAddressWithResidency> parseAddresses(String addressBody)
            throws AddressProcessingException {
        List<CanonicalAddressWithResidency> addresses;
        try {
            ObjectMapper mapper =
                    new ObjectMapper()
                            .registerModule(new Jdk8Module())
                            .registerModule(new JavaTimeModule())
                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            addresses = mapper.readValue(addressBody, new TypeReference<>() {});

        } catch (JsonProcessingException e) {
            throw new AddressProcessingException(
                    "could not parse addresses..." + e.getMessage(), e);
        }

        return addresses;
    }

    public AddressSessionItem getSession(String sessionId) {
        return dataStore.getItem(sessionId);
    }

    public void validateSessionId(String sessionId)
            throws SessionValidationException, SessionNotFoundException, SessionExpiredException {

        AddressSessionItem sessionItem = dataStore.getItem(sessionId);
        if (sessionItem == null) {
            throw new SessionNotFoundException("session not found");
        }

        if (sessionItem.getExpiryDate() < clock.instant().getEpochSecond()) {
            throw new SessionExpiredException("session expired");
        }
    }

    public AddressSessionItem saveAddresses(
            String sessionId, List<CanonicalAddressWithResidency> addresses)
            throws SessionExpiredException, SessionValidationException, SessionNotFoundException {
        validateSessionId(sessionId);

        var sessionItem = dataStore.getItem(sessionId);
        if (sessionItem == null) {
            throw new SessionNotFoundException("session not found");
        }

        sessionItem.setAddresses(addresses);
        sessionItem.setAuthorizationCode(UUID.randomUUID());
        dataStore.update(sessionItem);

        return sessionItem;
    }

    public TokenResponse createToken(TokenRequest tokenRequest) {
        AccessToken accessToken =
                new BearerAccessToken(
                        configurationService.getBearerAccessTokenTtl(), tokenRequest.getScope());
        return new AccessTokenResponse(new Tokens(accessToken, null));
    }

    public void writeToken(
            AccessTokenResponse tokenResponse, AddressSessionItem addressSessionItem) {
        addressSessionItem.setAccessToken(
                tokenResponse.getTokens().getBearerAccessToken().toAuthorizationHeader());

        dataStore.update(addressSessionItem);
    }

    public AddressSessionItem getAddressSessionItemByValue(final String value) {
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

        return getValueOrThrow(item);
    }

    private <T> T getValueOrThrow(List<T> list) {
        if (list.size() == 1) return list.get(0);

        throw new IllegalArgumentException(
                String.format("Parameter must have exactly one value: %s", list));
    }
}
