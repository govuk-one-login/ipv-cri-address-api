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
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exceptions.ServerException;
import uk.gov.di.ipv.cri.address.library.exceptions.ValidationException;
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
            throws ValidationException, ServerException {

        try {

            SessionRequest sessionRequest =
                    new ObjectMapper().readValue(requestBody, SessionRequest.class);

            String clientId = sessionRequest.getClientId();
            Map<String, String> clientAuthNConfig = getClientAuthenticationConfig(clientId);

            if (clientAuthNConfig == null || clientAuthNConfig.isEmpty()) {
                throw new ValidationException("no configuration for client id");
            }
            verifyRequestUri(sessionRequest, clientAuthNConfig);

            SignedJWT signedJWT = SignedJWT.parse(sessionRequest.getRequestJWT());
            verifyJWTHeader(clientAuthNConfig, signedJWT);
            verifyJWTClaimsSet(clientAuthNConfig, signedJWT);
            verifyJWTSignature(clientAuthNConfig, signedJWT);

            return sessionRequest;

        } catch (NullPointerException e) {
            throw new ValidationException("could not parse session request", e);
        } catch (ParseException e) {
            throw new ValidationException("could not parse JWT", e);
        } catch (JOSEException e) {
            throw new ValidationException("JWT signature verification failed", e);
        } catch (JsonProcessingException e) {
            throw new ValidationException("could not parse request body", e);
        } catch (CertificateException e) {
            throw new ServerException("could not parse certificate from config", e);
        } catch (BadJWTException e) {
            throw new ValidationException("Invalid JWT ClaimsSet", e);
        }
    }

    private Map<String, String> getClientAuthenticationConfig(String clientId) {
        String path = String.format(SSM_PARAM_CLIENT_JWT_AUTH_PATH, clientId);
        Map<String, String> clientConfig = configurationService.getParametersForPath(path);
        return clientConfig;
    }

    private void verifyRequestUri(SessionRequest sessionRequest, Map<String, String> clientConfig)
            throws ValidationException {
        URI redirectUri = URI.create(clientConfig.get("redirectUri"));
        if (sessionRequest.getRedirectUri() == null
                || !sessionRequest.getRedirectUri().equals(redirectUri)) {
            throw new ValidationException("redirect uri does not match configuration");
        }
    }

    private void verifyJWTHeader(Map<String, String> authenticationMap, SignedJWT signedJWT)
            throws ValidationException {
        JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(authenticationMap.get("authenticationAlg"));
        if (jwsAlgorithm != signedJWT.getHeader().getAlgorithm()) {
            throw new ValidationException("jwsAlgorithm does not match configuration");
        }
    }

    private void verifyJWTSignature(Map<String, String> authenticationMap, SignedJWT signedJWT)
            throws CertificateException, JOSEException, ValidationException {
        String publicCertificateToVerify = authenticationMap.get("publicCertificateToVerify");
        Certificate certificateFromConfig = getCertificateFromConfig(publicCertificateToVerify);

        if (!validSignature(signedJWT, certificateFromConfig)) {
            throw new ValidationException("JWT signature verification failed");
        }
    }

    private void verifyJWTClaimsSet(Map<String, String> clientAuthNConfig, SignedJWT signedJWT)
            throws BadJWTException, ParseException {
        DefaultJWTClaimsVerifier<?> verifier =
                new DefaultJWTClaimsVerifier<>(
                        new JWTClaimsSet.Builder().issuer(clientAuthNConfig.get("issuer")).build(),
                        new HashSet<>(Arrays.asList("exp", "nbf")));

        verifier.verify(signedJWT.getJWTClaimsSet(), null);
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
