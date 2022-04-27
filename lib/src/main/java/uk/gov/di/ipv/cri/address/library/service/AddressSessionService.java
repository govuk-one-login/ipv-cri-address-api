package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.constants.RequiredClaims;
import uk.gov.di.ipv.cri.address.library.domain.RawSessionRequest;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.net.URI;
import java.text.ParseException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AddressSessionService {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String CLIENT_ID = "client_id";

    private final ConfigurationService configurationService;
    private final DataStore<AddressSessionItem> dataStore;
    private final JWTDecrypter jwtDecrypter;
    private final Clock clock;
    private final JWTVerifier jwtVerifier;
    private final ObjectMapper objectMapper;
    private ObjectReader addressReader;

    @ExcludeFromGeneratedCoverageReport
    public AddressSessionService() {
        this.configurationService = new ConfigurationService();
        this.dataStore =
                new DataStore<>(
                        configurationService.getAddressSessionTableName(),
                        AddressSessionItem.class,
                        DataStore.getClient());
        this.clock = Clock.systemUTC();
        jwtVerifier = new JWTVerifier();
        String encryptionKeyId = configurationService.getKmsEncryptionKeyId();
        this.jwtDecrypter = new JWTDecrypter(new KMSRSADecrypter(encryptionKeyId));
        this.objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
    }

    public AddressSessionService(
            DataStore<AddressSessionItem> dataStore,
            ConfigurationService configurationService,
            Clock clock,
            JWTVerifier jwtVerifier,
            JWTDecrypter jwtDecrypter,
            ObjectMapper objectMapper) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.clock = clock;
        this.jwtVerifier = jwtVerifier;
        this.jwtDecrypter = jwtDecrypter;
        this.objectMapper = objectMapper;
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
        addressSessionItem.setSubject(sessionRequest.getSubject());

        dataStore.create(addressSessionItem);

        return addressSessionItem.getSessionId();
    }

    public SessionRequest validateSessionRequest(String requestBody)
            throws SessionValidationException, ClientConfigurationException {
        SessionRequest sessionRequest = parseSessionRequest(requestBody);

        Map<String, String> clientAuthenticationConfig =
                getClientAuthenticationConfig(sessionRequest.getClientId());

        verifyRequestUri(sessionRequest.getRedirectUri(), clientAuthenticationConfig);

        jwtVerifier.verifyJWT(
                clientAuthenticationConfig,
                sessionRequest.getSignedJWT(),
                List.of(
                        RequiredClaims.EXP.value,
                        RequiredClaims.SUB.value,
                        RequiredClaims.NBF.value));

        return sessionRequest;
    }

    private SessionRequest parseSessionRequest(String requestBody)
            throws SessionValidationException {
        try {
            RawSessionRequest rawSessionRequest =
                    this.objectMapper.readValue(requestBody, RawSessionRequest.class);
            SignedJWT requestJWT = decryptSessionRequest(rawSessionRequest.getRequestJWT());

            if (Objects.isNull(requestJWT)) {
                throw new SessionValidationException("could not parse request body to signed JWT");
            }

            JWTClaimsSet jwtClaims = requestJWT.getJWTClaimsSet();

            SessionRequest sessionRequest = new SessionRequest();
            sessionRequest.setAudience(jwtClaims.getAudience().get(0));
            sessionRequest.setClientId(rawSessionRequest.getClientId());
            sessionRequest.setJwtClientId(jwtClaims.getStringClaim(CLIENT_ID));
            sessionRequest.setExpirationTime(jwtClaims.getExpirationTime());
            sessionRequest.setIssuer(jwtClaims.getIssuer());
            sessionRequest.setNotBeforeTime(jwtClaims.getNotBeforeTime());
            sessionRequest.setRedirectUri(jwtClaims.getURIClaim(REDIRECT_URI));
            sessionRequest.setResponseType(jwtClaims.getStringClaim("response_type"));
            sessionRequest.setSignedJWT(requestJWT);
            sessionRequest.setState(jwtClaims.getStringClaim("state"));
            sessionRequest.setSubject(jwtClaims.getSubject());

            return sessionRequest;
        } catch (JsonProcessingException | ParseException e) {
            LOGGER.error("Failed to parse Session request", e);
            throw new SessionValidationException("could not parse request body", e);
        }
    }

    private SignedJWT decryptSessionRequest(String serialisedJWE)
            throws SessionValidationException {
        try {
            return jwtDecrypter.decrypt(serialisedJWE);
        } catch (ParseException e) {
            throw new SessionValidationException("Failed to parse request body", e);
        } catch (JOSEException e) {
            throw new SessionValidationException("Decryption failed", e);
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

    private void verifyRequestUri(URI requestRedirectUri, Map<String, String> clientConfig)
            throws SessionValidationException {
        URI configRedirectUri = URI.create(clientConfig.get("redirectUri"));
        if (requestRedirectUri == null || !requestRedirectUri.equals(configRedirectUri)) {
            throw new SessionValidationException(
                    "redirect uri "
                            + requestRedirectUri
                            + " does not match configuration uri "
                            + configRedirectUri);
        }
    }

    public List<CanonicalAddress> parseAddresses(String addressBody)
            throws AddressProcessingException {
        List<CanonicalAddress> addresses;
        try {
            addresses = getAddressReader().readValue(addressBody);
        } catch (JsonProcessingException e) {
            throw new AddressProcessingException(
                    "could not parse addresses..." + e.getMessage(), e);
        }

        return addresses;
    }

    public AddressSessionItem getSession(String sessionId) {
        return dataStore.getItem(sessionId);
    }

    public void update(AddressSessionItem addressSessionItem) {
        dataStore.update(addressSessionItem);
    }

    public void validateSessionId(String sessionId)
            throws SessionNotFoundException, SessionExpiredException {

        AddressSessionItem sessionItem = dataStore.getItem(sessionId);
        if (sessionItem == null) {
            throw new SessionNotFoundException("session not found");
        }

        if (sessionItem.getExpiryDate() < clock.instant().getEpochSecond()) {
            throw new SessionExpiredException("session expired");
        }
    }

    public AddressSessionItem saveAddresses(String sessionId, List<CanonicalAddress> addresses)
            throws SessionExpiredException, SessionNotFoundException {
        validateSessionId(sessionId);

        var sessionItem = dataStore.getItem(sessionId);
        if (sessionItem == null) {
            throw new SessionNotFoundException("session not found");
        }

        sessionItem.setAddresses(addresses);
        sessionItem.setAuthorizationCode(UUID.randomUUID().toString());
        dataStore.update(sessionItem);

        return sessionItem;
    }

    public AddressSessionItem getItemByGSIIndex(final String value, String indexName) {
        DynamoDbTable<AddressSessionItem> addressSessionTable = dataStore.getTable();
        DynamoDbIndex<AddressSessionItem> index = addressSessionTable.index(indexName);
        var listHelper = new ListUtil();

        return listHelper.getOneItemOrThrowError(dataStore.getItemByGsi(index, value));
    }

    private ObjectReader getAddressReader() {
        if (Objects.isNull(this.addressReader)) {
            this.addressReader =
                    this.objectMapper
                            .readerForListOf(CanonicalAddress.class)
                            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        return this.addressReader;
    }
}
