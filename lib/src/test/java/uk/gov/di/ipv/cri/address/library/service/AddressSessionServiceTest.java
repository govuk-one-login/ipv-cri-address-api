package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exceptions.ServerException;
import uk.gov.di.ipv.cri.address.library.exceptions.ValidationException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        addressSessionService.createAndSaveAddressSession(sessionRequest);
        verify(mockDataStore).create(mockAddressSessionItem.capture());
        AddressSessionItem capturedValue = mockAddressSessionItem.getValue();
        assertTrue(capturedValue.getSessionId() instanceof UUID);
        assertThat(capturedValue.getExpiryDate(), equalTo(fixedInstant.getEpochSecond() + 1));
        assertThat(capturedValue.getClientId(), equalTo("a client id"));
        assertThat(capturedValue.getState(), equalTo("state"));
        assertThat(
                capturedValue.getRedirectUri(),
                equalTo(URI.create("https://www.example.com/callback")));
    }

    @Test
    void shouldThrowValidationExceptionWhenSessionRequestIsInvalid() {

        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(Map.of("not", "a-session-request")));
                        });
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
        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(sessionRequest));
                        });
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
                .thenReturn(correctConfigMap(signedJWTBuilder));
        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(sessionRequest));
                        });
        assertThat(
                exception.getMessage(),
                containsString("redirect uri does not match configuration"));
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
                .thenReturn(correctConfigMap(signedJWTBuilder));
        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(sessionRequest));
                        });
        assertThat(exception.getMessage(), containsString("could not parse JWT"));
    }

    @Test
    void shouldValidateSignedJWT() throws IOException, ValidationException, ServerException {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        Map<String, String> configMap = correctConfigMap(signedJWTBuilder);
        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(configMap);

        SessionRequest result =
                addressSessionService.validateSessionRequest(marshallToJSON(sessionRequest));
        assertThat(result.getState(), equalTo(sessionRequest.getState()));
        assertThat(result.getClientId(), equalTo(sessionRequest.getClientId()));
        assertThat(result.getRedirectUri(), equalTo(sessionRequest.getRedirectUri()));
        assertThat(result.getResponseType(), equalTo(sessionRequest.getResponseType()));
    }

    private Map<String, String> correctConfigMap(SessionRequestBuilder.SignedJWTBuilder builder) {
        try {
            return Map.of(
                    "redirectUri", "https://www.example/com/callback",
                    "authenticationAlg", "RS256",
                    "issuer", "ipv-core",
                    "publicCertificateToVerify",
                            Base64.getEncoder()
                                    .encodeToString(builder.getCertificate().getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getRequestBody(String file) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(file);
        return new String(inputStream.readAllBytes());
    }

    private String marshallToJSON(Object sessionRequest) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(sessionRequest);
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
}
