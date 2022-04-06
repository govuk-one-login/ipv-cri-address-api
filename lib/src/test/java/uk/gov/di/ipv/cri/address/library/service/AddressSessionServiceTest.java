package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressSessionServiceTest {

    private static Instant fixedInstant;
    private AddressSessionService addressSessionService;

    @Mock private DataStore<AddressSessionItem> mockDataStore;
    @Mock private ConfigurationService mockConfigurationService;
    @Captor private ArgumentCaptor<AddressSessionItem> mockAddressSessionItem;

    @Test
    void shouldCallCreateOnAddressSessionDataStore() {
        when(mockConfigurationService.getAddressSessionTtl()).thenReturn(1L);
        SessionRequest sessionRequest = mock(SessionRequest.class);

        when(sessionRequest.getClientId()).thenReturn("a client id");
        when(sessionRequest.getState()).thenReturn("state");
        when(sessionRequest.getRedirectUri())
                .thenReturn(URI.create("https://www.example.com/callback"));
        when(sessionRequest.getSubject()).thenReturn("a subject");

        addressSessionService.createAndSaveAddressSession(sessionRequest);
        verify(mockDataStore).create(mockAddressSessionItem.capture());
        AddressSessionItem capturedValue = mockAddressSessionItem.getValue();
        assertNotNull(capturedValue.getSessionId());
        assertThat(capturedValue.getExpiryDate(), equalTo(fixedInstant.getEpochSecond() + 1));
        assertThat(capturedValue.getClientId(), equalTo("a client id"));
        assertThat(capturedValue.getState(), equalTo("state"));
        assertThat(capturedValue.getSubject(), equalTo("a subject"));
        assertThat(
                capturedValue.getRedirectUri(),
                equalTo(URI.create("https://www.example.com/callback")));
    }

    @Test
    void shouldThrowValidationExceptionWhenSessionRequestIsInvalid() {

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(Map.of("not", "a-session-request"))));
        assertThat(exception.getMessage(), containsString("could not parse request body"));
        verifyNoInteractions(mockConfigurationService);
    }

    @Test
    void shouldThrowValidationExceptionWhenRequestClientIdIsInvalid() {

        SessionRequestBuilder sessionRequestBuilder =
                new SessionRequestBuilder().withClientId("bad-client-id");
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);
        when(mockConfigurationService.getParametersForPath(
                        "/clients/bad-client-id/jwtAuthentication"))
                .thenReturn(Map.of());
        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));
        assertThat(exception.getMessage(), containsString("no configuration for client id"));
        verify(mockConfigurationService)
                .getParametersForPath("/clients/bad-client-id/jwtAuthentication");
    }

    @Test
    void shouldThrowValidationExceptionWhenRequestUriIsInvalid() {

        SessionRequestBuilder sessionRequestBuilder =
                new SessionRequestBuilder()
                        .withRedirectUri(URI.create("https://www.example.com/not-valid-callback"));
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));
        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));
        assertThat(
                exception.getMessage(),
                containsString(
                        "redirect uri https://www.example.com/not-valid-callback does not match configuration uri https://www.example/com/callback"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTIsInvalid() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);
        sessionRequest.setRequestJWT(
                Base64.getEncoder().encodeToString("not a jwt".getBytes(StandardCharsets.UTF_8)));

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));
        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));
        assertThat(exception.getMessage(), containsString("Could not parse request JWT"));
    }

    @Test
    void shouldThrowValidationExceptionWhenClientX509CertDoesNotMatchPrivateKey() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder()
                        .setCertificateFile("wrong-cert.crt.pem");
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));

        assertThat(exception.getMessage(), containsString("JWT signature verification failed"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTHeaderDoesNotMatchConfig() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder()
                        .setSigningAlgorithm(JWSAlgorithm.RS512);
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));

        assertThat(
                exception.getMessage(),
                containsString(
                        "jwt signing algorithm RS512 does not match signing algorithm configured for client: RS256"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTIsExpired() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder()
                        .setNow(Instant.now().minus(1, ChronoUnit.DAYS));
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));

        assertThat(exception.getMessage(), containsString("could not parse JWT"));
    }

    @Test
    void shouldValidateJWTSignedWithRSAKey()
            throws IOException, SessionValidationException, ClientConfigurationException {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        SessionRequest result =
                addressSessionService.validateSessionRequest(marshallToJSON(sessionRequest));
        assertThat(result.getState(), equalTo(sessionRequest.getState()));
        assertThat(result.getClientId(), equalTo(sessionRequest.getClientId()));
        assertThat(result.getRedirectUri(), equalTo(sessionRequest.getRedirectUri()));
        assertThat(result.getResponseType(), equalTo(sessionRequest.getResponseType()));
    }

    @Test
    void shouldValidateJWTSignedWithECKey()
            throws IOException, SessionValidationException, ClientConfigurationException {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        signedJWTBuilder.setPrivateKeyFile("signing_ec.pk8");
        signedJWTBuilder.setCertificateFile("signing_ec.crt.pem");
        signedJWTBuilder.setSigningAlgorithm(JWSAlgorithm.ES384);
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        Map<String, String> configMap = standardSSMConfigMap(signedJWTBuilder);
        configMap.put("authenticationAlg", "ES384");
        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(configMap);

        SessionRequest result =
                addressSessionService.validateSessionRequest(marshallToJSON(sessionRequest));
        assertThat(result.getState(), equalTo(sessionRequest.getState()));
        assertThat(result.getClientId(), equalTo(sessionRequest.getClientId()));
        assertThat(result.getRedirectUri(), equalTo(sessionRequest.getRedirectUri()));
        assertThat(result.getResponseType(), equalTo(sessionRequest.getResponseType()));
    }

    @Test
    void shouldGetAddressSessionItemByAuthorizationCodeIndexSuccessfully() {
        String authCodeValue = UUID.randomUUID().toString();
        AddressSessionItem item = new AddressSessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAuthorizationCode(authCodeValue);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockDataStore.getItemByGsi(mockAuthorizationCodeIndex, authCodeValue))
                .thenReturn(List.of(item));
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        AddressSessionItem addressSessionItem =
                addressSessionService.getItemByGSIIndex(
                        authCodeValue, AddressSessionItem.AUTHORIZATION_CODE_INDEX);
        assertThat(item.getSessionId(), equalTo(addressSessionItem.getSessionId()));
        assertThat(item.getAuthorizationCode(), equalTo(addressSessionItem.getAuthorizationCode()));
    }

    @Test
    void shouldGetAddressSessionItemByTokenIndexSuccessfully() {
        String accessTokenValue = UUID.randomUUID().toString();
        AddressSessionItem item = new AddressSessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAccessToken(accessTokenValue);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockTokenIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockDataStore.getItemByGsi(mockTokenIndex, accessTokenValue))
                .thenReturn(List.of(item));
        when(mockAddressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX))
                .thenReturn(mockTokenIndex);

        AddressSessionItem addressSessionItem =
                addressSessionService.getItemByGSIIndex(
                        accessTokenValue, AddressSessionItem.ACCESS_TOKEN_INDEX);
        assertThat(item.getSessionId(), equalTo(addressSessionItem.getSessionId()));
        assertThat(item.getAccessToken(), equalTo(addressSessionItem.getAccessToken()));
    }

    @BeforeAll
    static void beforeAll() {
        fixedInstant = Instant.now();
    }

    @BeforeEach
    void setUp() {
        Clock nowClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        addressSessionService =
                new AddressSessionService(mockDataStore, mockConfigurationService, nowClock);
    }

    @Test
    void parseAddresses() throws AddressProcessingException {
        String addresses =
                "[\n"
                        + "   {\n"
                        + "      \"uprn\": \"72262801\",\n"
                        + "      \"buildingNumber\": \"8\",\n"
                        + "      \"thoroughfareName\": \"GRANGE FIELDS WAY\",\n"
                        + "      \"postTown\": \"LEEDS\",\n"
                        + "      \"postcode\": \"LS10 4QL\",\n"
                        + "      \"countryCode\": \"GBR\",\n"
                        + "      \"residentFrom\": 1267142400000,\n"
                        + "      \"residentTo\": 1610755200000\n"
                        + "   },\n"
                        + "   {\n"
                        + "      \"uprn\": \"63094965\",\n"
                        + "      \"buildingNumber\": \"15\",\n"
                        + "      \"dependentLocality\": \"LOFTHOUSE\",\n"
                        + "      \"thoroughfareName\": \"RIDINGS LANE\",\n"
                        + "      \"postTown\": \"WAKEFIELD\",\n"
                        + "      \"postcode\": \"WF3 3SE\",\n"
                        + "      \"countryCode\": \"GBR\",\n"
                        + "      \"residentFrom\": 1610755200000,\n"
                        + "      \"residentTo\": 1627862400000\n"
                        + "   },\n"
                        + "   {\n"
                        + "      \"uprn\": \"63042351\",\n"
                        + "      \"buildingNumber\": \"5\",\n"
                        + "      \"thoroughfareName\": \"GATEWAYS\",\n"
                        + "      \"postTown\": \"WAKEFIELD\",\n"
                        + "      \"postcode\": \"WF1 2LZ\",\n"
                        + "      \"countryCode\": \"GBR\",\n"
                        + "      \"residentFrom\": 1627862400000,\n"
                        + "      \"currentResidency\": true\n"
                        + "   }\n"
                        + "]";
        List<CanonicalAddressWithResidency> parsedAddresses =
                addressSessionService.parseAddresses(addresses);
        assertThat(parsedAddresses.size(), equalTo(3));
        assertThat(parsedAddresses.get(0).getUprn().orElse(null), equalTo("72262801"));
        assertThat(parsedAddresses.get(0).getCurrentResidency().isPresent(), equalTo(false));
        assertThat(
                parsedAddresses.get(0).getResidentFrom().orElse(new Date()),
                equalTo(Date.from(Instant.parse("2010-02-26T00:00:00.00Z"))));

        assertThat(parsedAddresses.get(1).getCurrentResidency().isPresent(), equalTo(false));
        assertThat(parsedAddresses.get(2).getCurrentResidency().isPresent(), equalTo(true));
        assertThat(parsedAddresses.get(2).getResidentTo().isPresent(), equalTo(false));
    }

    @Test
    void saveAddressesSetsAuthorizationCode()
            throws SessionExpiredException, SessionNotFoundException {
        List<CanonicalAddressWithResidency> addresses = new ArrayList<>();
        CanonicalAddressWithResidency address1 = new CanonicalAddressWithResidency();
        address1.setUprn("72262801");
        address1.setBuildingNumber("8");
        address1.setThoroughfareName("GRANGE FIELDS WAY");
        address1.setPostTown("LEEDS");
        address1.setPostcode("LS10 4QL");
        address1.setCountryCode("GBR");
        address1.setResidentFrom(Date.from(Instant.parse("2010-02-26T00:00:00.00Z")));
        address1.setResidentTo(Date.from(Instant.parse("2021-01-16T00:00:00.00Z")));

        CanonicalAddressWithResidency address2 = new CanonicalAddressWithResidency();
        address2.setUprn("63094965");
        address2.setBuildingNumber("15");
        address2.setThoroughfareName("RIDINGS LANE");
        address2.setDependentLocality("LOFTHOUSE");
        address2.setPostTown("WAKEFIELD");
        address2.setPostcode("WF3 3SE");
        address2.setCountryCode("GBR");
        address2.setResidentFrom(Date.from(Instant.parse("2021-01-16T00:00:00.00Z")));
        address2.setResidentTo(Date.from(Instant.parse("2021-08-02T00:00:00.00Z")));

        CanonicalAddressWithResidency address3 = new CanonicalAddressWithResidency();
        address3.setUprn("63042351");
        address3.setBuildingNumber("5");
        address3.setThoroughfareName("GATEWAYS");
        address3.setPostTown("WAKEFIELD");
        address3.setPostcode("WF1 2LZ");
        address3.setCountryCode("GBR");
        address3.setResidentFrom(Date.from(Instant.parse("2021-08-02T00:00:00.00Z")));
        address3.setCurrentResidency(true);

        addresses.add(address1);
        addresses.add(address2);
        addresses.add(address3);

        AddressSessionItem addressSessionItem = new AddressSessionItem();
        addressSessionItem.setExpiryDate(
                Date.from(fixedInstant.plus(Duration.ofDays(1))).getTime());

        when(addressSessionService.getSession(anyString())).thenReturn(addressSessionItem);

        addressSessionService.saveAddresses(String.valueOf(UUID.randomUUID()), addresses);
        verify(mockDataStore).update(mockAddressSessionItem.capture());
        assertThat(mockAddressSessionItem.getValue().getAddresses(), equalTo(addresses));
        assertThat(mockAddressSessionItem.getValue().getAuthorizationCode(), notNullValue());
    }

    @Test
    void saveAddressesThrowsSessionExpired() {
        List<CanonicalAddressWithResidency> addresses = new ArrayList<>();

        AddressSessionItem addressSessionItem = new AddressSessionItem();

        when(addressSessionService.getSession(anyString())).thenReturn(addressSessionItem);

        assertThrows(
                SessionExpiredException.class,
                () ->
                        addressSessionService.saveAddresses(
                                String.valueOf(UUID.randomUUID()), addresses));
    }

    @Test
    void saveAddressesThrowsSessionNotFound() {
        List<CanonicalAddressWithResidency> addresses = new ArrayList<>();
        assertThrows(
                SessionNotFoundException.class,
                () ->
                        addressSessionService.saveAddresses(
                                String.valueOf(UUID.randomUUID()), addresses));
    }

    private Map<String, String> standardSSMConfigMap(
            SessionRequestBuilder.SignedJWTBuilder builder) {
        try {

            HashMap<String, String> map = new HashMap<>();
            map.put("redirectUri", "https://www.example/com/callback");
            map.put("authenticationAlg", "RS256");
            map.put("issuer", "ipv-core");
            map.put(
                    "publicCertificateToVerify",
                    Base64.getEncoder().encodeToString(builder.getCertificate().getEncoded()));
            return map;
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String marshallToJSON(Object sessionRequest) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(sessionRequest);
    }
}
