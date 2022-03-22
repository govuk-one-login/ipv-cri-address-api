package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class SessionRequestBuilder {

    private final SessionRequest sessionRequest;

    public SessionRequestBuilder() {
        this.sessionRequest = new SessionRequest();
        sessionRequest.setClientId("ipv-core");
        sessionRequest.setRedirectUri(URI.create("https://www.example/com/callback"));
        sessionRequest.setResponseType("code");
        sessionRequest.setState("state");
    }

    public SessionRequestBuilder withClientId(String clientId) {
        sessionRequest.setClientId(clientId);
        return this;
    }

    public SessionRequestBuilder withRedirectUri(URI uri) {
        sessionRequest.setRedirectUri(uri);
        return this;
    }

    public SessionRequest build(SignedJWTBuilder jwtBuilder) {
        SignedJWT jwt = jwtBuilder.build();
        sessionRequest.setRequestJWT(jwt.serialize());
        return sessionRequest;
    }

    static class SignedJWTBuilder {

        private String issuer = "ipv-core";
        private Instant now = Instant.now();
        private JWSAlgorithm signingAlgorithm = JWSAlgorithm.RS256;
        private Certificate certificate = null;
        private String certificateFile = "address-cri-test.crt.pem";
        private String privateKeyFile = "address-cri-test.pk8";

        public SignedJWTBuilder setNow(Instant now) {
            this.now = now;
            return this;
        }

        public SignedJWTBuilder setSigningAlgorithm(JWSAlgorithm signingAlgorithm) {
            this.signingAlgorithm = signingAlgorithm;
            return this;
        }

        public SignedJWTBuilder setCertificateFile(String certificateFile) {
            this.certificateFile = certificateFile;
            return this;
        }

        public SignedJWTBuilder setPrivateKeyFile(String privateKeyFile) {
            this.privateKeyFile = privateKeyFile;
            return this;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Certificate getCertificate() {
            return certificate;
        }

        private Certificate generateCertificate(String resourceName) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                return cf.generateCertificate(is);
            } catch (CertificateException | IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public SignedJWT build() {
            try {

                PrivateKey privateKey = getPrivateKeyFromResources(privateKeyFile);
                certificate = generateCertificate(certificateFile);

                String kid = UUID.randomUUID().toString();
                String ipv_session_id = UUID.randomUUID().toString();

                SignedJWT signedJWT =
                        new SignedJWT(
                                new JWSHeader.Builder(signingAlgorithm).keyID(kid).build(),
                                new JWTClaimsSet.Builder()
                                        .subject(ipv_session_id)
                                        .issueTime(Date.from(now))
                                        .issuer(issuer)
                                        .notBeforeTime(Date.from(now))
                                        .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                                        .claim("claims", Map.of("vc_http_api", Map.of()))
                                        .build());

                if (privateKey instanceof RSAPrivateKey) {
                    signedJWT.sign(new RSASSASigner(privateKey));
                } else {
                    signedJWT.sign(new ECDSASigner((ECPrivateKey) privateKey));
                }

                return signedJWT;
            } catch (JOSEException e) {
                throw new IllegalStateException(e);
            }
        }

        private PrivateKey getPrivateKeyFromResources(String resourceName) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                assert is != null;
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(is.readAllBytes());
                if (this.signingAlgorithm.toString().startsWith("RS")) {
                    return KeyFactory.getInstance("RSA").generatePrivate(spec);
                } else {
                    return KeyFactory.getInstance("EC").generatePrivate(spec);
                }

            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
