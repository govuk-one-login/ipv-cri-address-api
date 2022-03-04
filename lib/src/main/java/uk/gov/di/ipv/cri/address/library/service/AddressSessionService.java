package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
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
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class AddressSessionService {

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
            String path = String.format("/clients/%s/jwtAuthentication", clientId);
            Map<String, String> authenticationMap = configurationService.getParametersForPath(path);
            if (authenticationMap == null || authenticationMap.isEmpty()) {
                throw new ValidationException(
                        String.format("no configuration for client id %s", clientId));
            }

            URI redirectUri = URI.create(authenticationMap.get("redirectUri"));
            if (sessionRequest.getRedirectUri() == null
                    || !sessionRequest.getRedirectUri().equals(redirectUri)) {
                throw new ValidationException("redirect uri does not match configuration");
            }

            JWSAlgorithm jwsAlgorithm =
                    JWSAlgorithm.parse(authenticationMap.get("authenticationAlg"));
            SignedJWT signedJWT = SignedJWT.parse(sessionRequest.getRequestJWT());
            if (jwsAlgorithm != signedJWT.getHeader().getAlgorithm()) {
                throw new ValidationException("jwsAlgorithm does not match configuration");
            }

            String publicCertificateToVerify = authenticationMap.get("publicCertificateToVerify");
            Certificate certificateFromConfig = getCertificateFromConfig(publicCertificateToVerify);

            if (!validSignature(signedJWT, certificateFromConfig)) {
                throw new ValidationException("JWT signature verification failed");
            }

            return sessionRequest;
        } catch (NullPointerException e) {
            throw new ValidationException("could not parse session request", e);
        } catch (ParseException e) {
            throw new ValidationException("could not parse JWT", e);
        } catch (JOSEException e) {
            throw new ValidationException("JWT signature verification failed", e);
        } catch (JsonProcessingException e) {
            throw new ValidationException("TODO", e);
        } catch (CertificateException e) {
            throw new ServerException("could not parse certificate from config", e);
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
