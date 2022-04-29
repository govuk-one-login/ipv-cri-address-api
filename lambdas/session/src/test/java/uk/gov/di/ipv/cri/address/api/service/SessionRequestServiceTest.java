package uk.gov.di.ipv.cri.address.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.api.domain.RawSessionRequest;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.address.library.service.JWTVerifier;

import java.io.IOException;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionRequestServiceTest {

    @Mock private ConfigurationService mockConfigurationService;
    @Mock private JWTDecrypter mockJwtDecrypter;
    @Mock private JWTVerifier mockJwtVerifier;
    @Mock private ObjectMapper mockObjectMapper;
    private SessionRequestService sessionRequestService;

    @BeforeEach
    void setUp() {
        sessionRequestService =
                new SessionRequestService(
                        mockObjectMapper,
                        mockJwtVerifier,
                        mockConfigurationService,
                        mockJwtDecrypter);
    }

    @Test
    void shouldThrowValidationExceptionWhenSessionRequestIsInvalid() throws IOException {
        String requestBody = marshallToJSON(Map.of("not", "a-session-request"));
        when(mockObjectMapper.readValue(requestBody, RawSessionRequest.class))
                .thenThrow(Mockito.mock(JsonProcessingException.class));
        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> sessionRequestService.validateSessionRequest(requestBody));
        assertThat(exception.getMessage(), containsString("Could not parse request body"));
        verifyNoInteractions(mockConfigurationService);
    }

    @Test
    void shouldThrowValidationExceptionWhenRequestClientIdIsInvalid()
            throws ParseException, JOSEException, JsonProcessingException {
        String invalidClientId = "invalid-client-id";
        String testRequestBody = "test-request-body";
        String configParameterPath = "/clients/" + invalidClientId + "/jwtAuthentication";
        SignedJWTBuilder signedJWTBuilder = new SignedJWTBuilder().setClientId(invalidClientId);
        SignedJWT signedJWT = signedJWTBuilder.build();
        RawSessionRequest rawSessionRequest = createRawSessionRequest(signedJWT);
        rawSessionRequest.setClientId(invalidClientId);
        when(mockObjectMapper.readValue(testRequestBody, RawSessionRequest.class))
                .thenReturn(rawSessionRequest);
        when(mockJwtDecrypter.decrypt(rawSessionRequest.getRequestJWT())).thenReturn(signedJWT);
        when(mockConfigurationService.getParametersForPath(configParameterPath))
                .thenReturn(Map.of());
        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> sessionRequestService.validateSessionRequest(testRequestBody));
        assertThat(exception.getMessage(), containsString("no configuration for client id"));
        verify(mockConfigurationService).getParametersForPath(configParameterPath);
    }

    @Test
    void shouldThrowValidationExceptionWhenRedirectUriIsInvalid()
            throws ParseException, JOSEException, JsonProcessingException {
        SignedJWTBuilder signedJWTBuilder =
                new SignedJWTBuilder().setRedirectUri("https://www.example.com/not-valid-callback");
        SignedJWT signedJWT = signedJWTBuilder.build();
        RawSessionRequest rawSessionRequest = createRawSessionRequest(signedJWTBuilder.build());
        String requestBody = "test request body";
        when(mockObjectMapper.readValue(requestBody, RawSessionRequest.class))
                .thenReturn(rawSessionRequest);
        when(mockJwtDecrypter.decrypt(rawSessionRequest.getRequestJWT())).thenReturn(signedJWT);
        initMockConfigurationService(standardSSMConfigMap(signedJWTBuilder.getCertificate()));

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> sessionRequestService.validateSessionRequest(requestBody));
        assertThat(
                exception.getMessage(),
                containsString(
                        "redirect uri https://www.example.com/not-valid-callback does not match configuration uri https://www.example/com/callback"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTIsInvalid()
            throws JsonProcessingException, ParseException, JOSEException {
        RawSessionRequest rawSessionRequest = createRawSessionRequest("not a jwt");

        String requestBody = "test request body";
        when(mockObjectMapper.readValue(requestBody, RawSessionRequest.class))
                .thenReturn(rawSessionRequest);
        when(mockJwtDecrypter.decrypt(rawSessionRequest.getRequestJWT())).thenReturn(null);

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> sessionRequestService.validateSessionRequest(requestBody));
        assertThat(
                exception.getMessage(),
                containsString("could not parse request body to signed JWT"));
    }

    @Test
    void shouldValidateJWTSignedWithECKey()
            throws IOException, SessionValidationException, ClientConfigurationException,
                    java.text.ParseException, JOSEException {
        String requestBody = "test request body";
        SignedJWTBuilder signedJWTBuilder =
                new SignedJWTBuilder()
                        .setPrivateKeyFile("signing_ec.pk8")
                        .setCertificateFile("signing_ec.crt.pem")
                        .setSigningAlgorithm(JWSAlgorithm.ES384);
        SignedJWT signedJWT = signedJWTBuilder.build();
        RawSessionRequest rawSessionRequest = createRawSessionRequest(signedJWT);
        when(mockObjectMapper.readValue(requestBody, RawSessionRequest.class))
                .thenReturn(rawSessionRequest);

        when(mockJwtDecrypter.decrypt(rawSessionRequest.getRequestJWT())).thenReturn(signedJWT);
        Map<String, String> configMap = standardSSMConfigMap(signedJWTBuilder.getCertificate());
        configMap.put("authenticationAlg", "ES384");
        initMockConfigurationService(configMap);

        SessionRequest result = sessionRequestService.validateSessionRequest(requestBody);

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

    private void initMockConfigurationService(Map<String, String> parameters) {
        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(parameters);
    }
}
