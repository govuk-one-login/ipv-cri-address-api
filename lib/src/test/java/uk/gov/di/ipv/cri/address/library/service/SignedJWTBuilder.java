package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;

import java.io.IOException;
import java.io.InputStream;
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

class SignedJWTBuilder {

    private String issuer = "ipv-core";
    private Instant now = Instant.now();
    private JWSAlgorithm signingAlgorithm = JWSAlgorithm.RS256;
    private Certificate certificate = null;
    private String certificateFile = "address-cri-test.crt.pem";
    private String privateKeyFile = "address-cri-test.pk8";
    private String redirectUri = "https://www.example/com/callback";
    private String clientId = "ipv-core";
    private Date notBeforeTime = Date.from(now);
    private String audience = "test-audience";
    private boolean includeSubject = true;

    SignedJWTBuilder setNow(Instant now) {
        this.now = now;
        return this;
    }

    SignedJWTBuilder setSigningAlgorithm(JWSAlgorithm signingAlgorithm) {
        this.signingAlgorithm = signingAlgorithm;
        return this;
    }

    SignedJWTBuilder setCertificateFile(String certificateFile) {
        this.certificateFile = certificateFile;
        return this;
    }

    SignedJWTBuilder setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
        return this;
    }

    SignedJWTBuilder setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    SignedJWTBuilder setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    SignedJWTBuilder setNotBeforeTime(Date notBeforeTime) {
        this.notBeforeTime = notBeforeTime;
        return this;
    }

    SignedJWTBuilder setAudience(String audience) {
        this.audience = audience;
        return this;
    }

    SignedJWTBuilder setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    SignedJWTBuilder setIncludeSubject(boolean includeSubject) {
        this.includeSubject = includeSubject;
        return this;
    }

    Certificate getCertificate() {
        return certificate;
    }

    SignedJWT build() {
        try {

            PrivateKey privateKey = getPrivateKeyFromResources(privateKeyFile);
            certificate = generateCertificate(certificateFile);

            String kid = UUID.randomUUID().toString();
            String ipv_session_id = UUID.randomUUID().toString();

            JWTClaimsSet.Builder jwtClaimSetBuilder =
                    new JWTClaimsSet.Builder()
                            .audience(audience)
                            .issueTime(Date.from(now))
                            .issuer(issuer)
                            .notBeforeTime(notBeforeTime)
                            .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                            .claim("claims", Map.of("vc_http_api", Map.of()))
                            .claim(
                                    "response_type",
                                    ResponseType.CODE.stream().findFirst().get().getValue())
                            .claim("client_id", clientId)
                            .claim("redirect_uri", redirectUri)
                            .claim("state", "state");

            if (includeSubject) {
                jwtClaimSetBuilder.subject(ipv_session_id);
            }

            SignedJWT signedJWT =
                    new SignedJWT(
                            new JWSHeader.Builder(signingAlgorithm).keyID(kid).build(),
                            jwtClaimSetBuilder.build());

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

    private Certificate generateCertificate(String resourceName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCertificate(is);
        } catch (CertificateException | IOException e) {
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
