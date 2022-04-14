package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import uk.gov.di.ipv.cri.address.library.domain.RawSessionRequest;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.io.IOException;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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
    @Mock private JWTVerifier jwtVerifier;
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
        String invalidClientId = "invalid-client-id";
        String configParameterPath = "/clients/" + invalidClientId + "/jwtAuthentication";
        SignedJWTBuilder signedJWTBuilder = new SignedJWTBuilder().setClientId(invalidClientId);
        RawSessionRequest rawSessionRequest = createRawSessionRequest(signedJWTBuilder.build());
        rawSessionRequest.setClientId(invalidClientId);

        when(mockConfigurationService.getParametersForPath(configParameterPath))
                .thenReturn(Map.of());
        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(rawSessionRequest)));
        assertThat(exception.getMessage(), containsString("no configuration for client id"));
        verify(mockConfigurationService).getParametersForPath(configParameterPath);
    }

    @Test
    void shouldThrowValidationExceptionWhenRedirectUriIsInvalid() {
        SignedJWTBuilder signedJWTBuilder =
                new SignedJWTBuilder().setRedirectUri("https://www.example.com/not-valid-callback");
        RawSessionRequest rawSessionRequest = createRawSessionRequest(signedJWTBuilder.build());

        initMockConfigurationService(signedJWTBuilder.getCertificate(), false);

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(rawSessionRequest)));
        assertThat(
                exception.getMessage(),
                containsString(
                        "redirect uri https://www.example.com/not-valid-callback does not match configuration uri https://www.example/com/callback"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTIsInvalid() {
        RawSessionRequest sessionRequest = createRawSessionRequest("not a jwt");

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () ->
                                addressSessionService.validateSessionRequest(
                                        marshallToJSON(sessionRequest)));
        assertThat(exception.getMessage(), containsString("Could not parse request JWT"));
    }

    @Test
    void shouldValidateJWTSignedWithECKey()
            throws IOException, SessionValidationException, ClientConfigurationException,
                    java.text.ParseException {
        SignedJWTBuilder signedJWTBuilder =
                new SignedJWTBuilder()
                        .setPrivateKeyFile("signing_ec.pk8")
                        .setCertificateFile("signing_ec.crt.pem")
                        .setSigningAlgorithm(JWSAlgorithm.ES384);
        SignedJWT signedJWT = signedJWTBuilder.build();
        RawSessionRequest rawSessionRequest = createRawSessionRequest(signedJWT);

        Map<String, String> configMap = standardSSMConfigMap(signedJWTBuilder.getCertificate());
        configMap.put("authenticationAlg", "ES384");
        initMockConfigurationService(configMap, false);

        SessionRequest result =
                addressSessionService.validateSessionRequest(marshallToJSON(rawSessionRequest));

        makeSessionRequestFieldValueAssertions(
                result, rawSessionRequest, signedJWT.getJWTClaimsSet());
    }

    private void makeSessionRequestFieldValueAssertions(
            SessionRequest sessionRequest,
            RawSessionRequest rawSessionRequest,
            JWTClaimsSet jwtClaims)
            throws java.text.ParseException {
        assertThat(sessionRequest.getAudience(), equalTo(jwtClaims.getAudience().get(0)));
        assertThat(sessionRequest.getIssuer(), equalTo(jwtClaims.getIssuer()));
        assertThat(sessionRequest.getSubject(), equalTo(jwtClaims.getSubject()));

        assertThat(sessionRequest.getState(), equalTo(jwtClaims.getStringClaim("state")));
        assertThat(sessionRequest.getClientId(), equalTo(rawSessionRequest.getClientId()));
        assertThat(sessionRequest.getClientId(), equalTo(jwtClaims.getStringClaim("client_id")));
        assertThat(
                sessionRequest.getRedirectUri(),
                equalTo(URI.create(jwtClaims.getStringClaim("redirect_uri"))));
        assertThat(
                sessionRequest.getResponseType(),
                equalTo(jwtClaims.getStringClaim("response_type")));
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
                new AddressSessionService(
                        mockDataStore, mockConfigurationService, nowClock, jwtVerifier);
    }

    @Test
    void parseAddresses() throws AddressProcessingException {
        String addresses =
                "[\n"
                        + "   {\n"
                        + "      \"uprn\": \"72262801\",\n"
                        + "      \"buildingNumber\": \"8\",\n"
                        + "      \"streetName\": \"GRANGE FIELDS WAY\",\n"
                        + "      \"postTown\": \"LEEDS\",\n"
                        + "      \"postcode\": \"LS10 4QL\",\n"
                        + "      \"countryCode\": \"GBR\",\n"
                        + "      \"validFrom\": \"2010-02-26\",\n"
                        + "      \"validUntil\": \"2021-01-16\"\n"
                        + "   },\n"
                        + "   {\n"
                        + "      \"uprn\": \"63094965\",\n"
                        + "      \"buildingNumber\": \"15\",\n"
                        + "      \"dependentLocality\": \"LOFTHOUSE\",\n"
                        + "      \"streetName\": \"RIDINGS LANE\",\n"
                        + "      \"postTown\": \"WAKEFIELD\",\n"
                        + "      \"postcode\": \"WF3 3SE\",\n"
                        + "      \"countryCode\": \"GBR\",\n"
                        + "      \"validFrom\": \"2021-01-16\",\n"
                        + "      \"validUntil\": \"2021-08-02\"\n"
                        + "   },\n"
                        + "   {\n"
                        + "      \"uprn\": \"63042351\",\n"
                        + "      \"buildingNumber\": \"5\",\n"
                        + "      \"streetName\": \"GATEWAYS\",\n"
                        + "      \"postTown\": \"WAKEFIELD\",\n"
                        + "      \"postcode\": \"WF1 2LZ\",\n"
                        + "      \"countryCode\": \"GBR\",\n"
                        + "      \"validFrom\": \"2021-08-02\"\n"
                        + "   }\n"
                        + "]";
        List<CanonicalAddress> parsedAddresses = addressSessionService.parseAddresses(addresses);
        assertThat(parsedAddresses.size(), equalTo(3));
        assertThat(parsedAddresses.get(0).getUprn().orElse(null), equalTo(72262801L));
        assertThat(
                parsedAddresses.get(0).getValidFrom().orElse(new Date()),
                equalTo(Date.from(Instant.parse("2010-02-26T00:00:00.00Z"))));

        assertThat(parsedAddresses.get(2).getValidUntil().isPresent(), equalTo(false));
    }

    @Test
    void saveAddressesSetsAuthorizationCode()
            throws SessionExpiredException, SessionNotFoundException {
        List<CanonicalAddress> addresses = new ArrayList<>();
        CanonicalAddress address1 = new CanonicalAddress();
        address1.setUprn(Long.valueOf("72262801"));
        address1.setBuildingNumber("8");
        address1.setStreetName("GRANGE FIELDS WAY");
        address1.setPostTown("LEEDS");
        address1.setPostcode("LS10 4QL");
        address1.setCountryCode("GBR");
        address1.setValidFrom(Date.from(Instant.parse("2010-02-26T00:00:00.00Z")));
        address1.setValidUntil(Date.from(Instant.parse("2021-01-16T00:00:00.00Z")));

        CanonicalAddress address2 = new CanonicalAddress();
        address2.setUprn(Long.valueOf("63094965"));
        address2.setBuildingNumber("15");
        address2.setStreetName("RIDINGS LANE");
        address2.setDependentLocality("LOFTHOUSE");
        address2.setPostTown("WAKEFIELD");
        address2.setPostcode("WF3 3SE");
        address2.setCountryCode("GBR");
        address2.setValidFrom(Date.from(Instant.parse("2021-01-16T00:00:00.00Z")));
        address2.setValidUntil(Date.from(Instant.parse("2021-08-02T00:00:00.00Z")));

        CanonicalAddress address3 = new CanonicalAddress();
        address3.setUprn(Long.valueOf("63042351"));
        address3.setBuildingNumber("5");
        address3.setStreetName("GATEWAYS");
        address3.setPostTown("WAKEFIELD");
        address3.setPostcode("WF1 2LZ");
        address3.setCountryCode("GBR");
        address3.setValidFrom(Date.from(Instant.parse("2021-08-02T00:00:00.00Z")));

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
        List<CanonicalAddress> addresses = new ArrayList<>();

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
        List<CanonicalAddress> addresses = new ArrayList<>();
        assertThrows(
                SessionNotFoundException.class,
                () ->
                        addressSessionService.saveAddresses(
                                String.valueOf(UUID.randomUUID()), addresses));
    }

    private Map<String, String> standardSSMConfigMap(Certificate certificate) {
        try {
            HashMap<String, String> map = new HashMap<>();
            map.put("redirectUri", "https://www.example/com/callback");
            map.put("authenticationAlg", "RS256");
            map.put("issuer", "ipv-core");
            map.put(
                    "publicCertificateToVerify",
                    Base64.getEncoder().encodeToString(certificate.getEncoded()));
            return map;
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String marshallToJSON(Object sessionRequest) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(sessionRequest);
    }

    private RawSessionRequest createRawSessionRequest(SignedJWT signedJWT) {
        return createRawSessionRequest(signedJWT.serialize());
    }

    private RawSessionRequest createRawSessionRequest(String serialisedJWT) {
        RawSessionRequest rawSessionRequest = new RawSessionRequest();
        rawSessionRequest.setClientId("ipv-core");
        rawSessionRequest.setRequestJWT(serialisedJWT);
        return rawSessionRequest;
    }

    private void initMockConfigurationService(Certificate certificate) {
        initMockConfigurationService(standardSSMConfigMap(certificate));
    }

    private void initMockConfigurationService(
            Certificate certificate, boolean stubGetAudienceMethod) {
        initMockConfigurationService(standardSSMConfigMap(certificate), stubGetAudienceMethod);
    }

    private void initMockConfigurationService(Map<String, String> parameters) {
        initMockConfigurationService(parameters, true);
    }

    private void initMockConfigurationService(
            Map<String, String> parameters, boolean stubGetAudienceMethod) {
        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(parameters);
        if (stubGetAudienceMethod) {
            when(mockConfigurationService.getAddressCriAudienceIdentifier())
                    .thenReturn("test-audience");
        }
    }
}
