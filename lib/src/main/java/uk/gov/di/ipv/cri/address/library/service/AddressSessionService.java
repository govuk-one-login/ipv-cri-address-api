package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exceptions.ServerException;
import uk.gov.di.ipv.cri.address.library.exceptions.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class AddressSessionService {

    public static final String SSM_PARAM_CLIENT_JWT_AUTH_PATH = "/clients/%s/jwtAuthentication";
    private final DataStore<AddressSessionItem> dataStore;
    private final ConfigurationService configurationService;
    private final Clock clock;

    public AddressSessionService() {
        this.configurationService = new ConfigurationService();
        this.dataStore =
                new DataStore<>(
                        this.configurationService.getAddressSessionTableName(),
                        AddressSessionItem.class,
                        DataStore.getClient());
        clock = Clock.systemUTC();
    }

    @ExcludeFromGeneratedCoverageReport
    public AddressSessionService(
            DataStore<AddressSessionItem> dataStore,
            ConfigurationService configurationService,
            Clock clock) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.clock = clock;
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
            throws SessionValidationException, ServerException {

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

    private SessionRequest parseSessionRequest(String requestBody) throws SessionValidationException {
        try {
            return new ObjectMapper().readValue(requestBody, SessionRequest.class);
        } catch (JsonProcessingException e) {
            throw new SessionValidationException("could not parse request body", e);
        }
    }

    private SignedJWT parseRequestJWT(SessionRequest sessionRequest) throws SessionValidationException {
        try {
            return SignedJWT.parse(sessionRequest.getRequestJWT());
        } catch (ParseException e) {
            throw new SessionValidationException("Could not parse request JWT", e);
        }
    }

    private Map<String, String> getClientAuthenticationConfig(String clientId)
            throws SessionValidationException {
        String path = String.format(SSM_PARAM_CLIENT_JWT_AUTH_PATH, clientId);
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

    private void verifyJWTHeader(Map<String, String> authenticationMap, SignedJWT signedJWT)
            throws SessionValidationException {
        JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(authenticationMap.get("authenticationAlg"));
        if (jwsAlgorithm != signedJWT.getHeader().getAlgorithm()) {
            throw new SessionValidationException("jwsAlgorithm does not match configuration");
        }
    }

    private void verifyJWTSignature(Map<String, String> authenticationMap, SignedJWT signedJWT)
            throws SessionValidationException, ServerException {
        String publicCertificateToVerify = authenticationMap.get("publicCertificateToVerify");
        try {
            Certificate certificateFromConfig = getCertificateFromConfig(publicCertificateToVerify);

            if (!validSignature(signedJWT, certificateFromConfig)) {
                throw new SessionValidationException("JWT signature verification failed");
            }
        } catch (JOSEException e) {
            throw new SessionValidationException("JWT signature verification failed", e);
        } catch (CertificateException e) {
            throw new ServerException(e);
        }
    }

    private void verifyJWTClaimsSet(Map<String, String> clientAuthNConfig, SignedJWT signedJWT)
            throws SessionValidationException {
        DefaultJWTClaimsVerifier<?> verifier =
                new DefaultJWTClaimsVerifier<>(
                        new JWTClaimsSet.Builder().issuer(clientAuthNConfig.get("issuer")).build(),
                        new HashSet<>(Arrays.asList("exp", "nbf")));

        try {
            verifier.verify(signedJWT.getJWTClaimsSet(), null);
        } catch (BadJWTException | ParseException e) {
            throw new SessionValidationException("could not parse JWT", e);
        }
    }

    private Certificate getCertificateFromConfig(String base64) throws CertificateException {
        byte[] binaryCertificate = Base64.getDecoder().decode(base64);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return factory.generateCertificate(new ByteArrayInputStream(binaryCertificate));
    }

    private boolean validSignature(SignedJWT signedJWT, Certificate clientCertificate)
            throws JOSEException {
        PublicKey publicKey = clientCertificate.getPublicKey();
        RSASSAVerifier rsassaVerifier = new RSASSAVerifier((RSAPublicKey) publicKey);
        return signedJWT.verify(rsassaVerifier);
    }
}
