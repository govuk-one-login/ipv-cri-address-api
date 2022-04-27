package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

class JWTVerifier {

    void verifyJWT(
            Map<String, String> clientAuthenticationConfig,
            SignedJWT signedJWT,
            List<String> requiredClaims)
            throws SessionValidationException, ClientConfigurationException {
        this.verifyJWTHeader(clientAuthenticationConfig, signedJWT);
        this.verifyJWTClaimsSet(clientAuthenticationConfig, signedJWT, requiredClaims);
        this.verifyJWTSignature(clientAuthenticationConfig, signedJWT);
    }

    private void verifyJWTHeader(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT)
            throws SessionValidationException {
        JWSAlgorithm configuredAlgorithm =
                JWSAlgorithm.parse(clientAuthenticationConfig.get("authenticationAlg"));
        JWSAlgorithm jwtAlgorithm = signedJWT.getHeader().getAlgorithm();
        if (jwtAlgorithm != configuredAlgorithm) {
            throw new SessionValidationException(
                    String.format(
                            "jwt signing algorithm %s does not match signing algorithm configured for client: %s",
                            jwtAlgorithm, configuredAlgorithm));
        }
    }

    private void verifyJWTSignature(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT)
            throws SessionValidationException, ClientConfigurationException {
        String publicCertificateToVerify =
                clientAuthenticationConfig.get("publicCertificateToVerify");
        try {
            JWSAlgorithm signingAlgorithm = signedJWT.getHeader().getAlgorithm();
            PublicKey pubicKeyFromConfig =
                    getPublicKeyFromConfig(publicCertificateToVerify, signingAlgorithm);
            if (!verifySignature(signedJWT, pubicKeyFromConfig)) {
                throw new SessionValidationException("JWT signature verification failed");
            }
        } catch (JOSEException | ParseException e) {
            throw new SessionValidationException("JWT signature verification failed", e);
        } catch (CertificateException e) {
            throw new ClientConfigurationException("Certificate problem encountered", e);
        }
    }

    private void verifyJWTClaimsSet(
            Map<String, String> clientAuthenticationConfig,
            SignedJWT signedJWT,
            List<String> requiredClaims)
            throws SessionValidationException {
        DefaultJWTClaimsVerifier<?> verifier =
                new DefaultJWTClaimsVerifier<>(
                        new JWTClaimsSet.Builder()
                                .issuer(clientAuthenticationConfig.get("issuer"))
                                .audience(clientAuthenticationConfig.get("audience"))
                                .build(),
                        new HashSet<>(requiredClaims));

        try {
            verifier.verify(signedJWT.getJWTClaimsSet(), null);
        } catch (BadJWTException | ParseException e) {
            throw new SessionValidationException(e.getMessage(), e);
        }
    }

    private PublicKey getPublicKeyFromConfig(
            String serialisedPublicKey, JWSAlgorithm signingAlgorithm)
            throws CertificateException, ParseException, JOSEException {
        if (JWSAlgorithm.Family.RSA.contains(signingAlgorithm)) {
            byte[] binaryCertificate = Base64.getDecoder().decode(serialisedPublicKey);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate certificate =
                    factory.generateCertificate(new ByteArrayInputStream(binaryCertificate));
            return certificate.getPublicKey();
        } else if (JWSAlgorithm.Family.EC.contains(signingAlgorithm)) {
            return new ECKey.Builder(ECKey.parse(serialisedPublicKey)).build().toECPublicKey();
        } else {
            throw new IllegalArgumentException(
                    "Unexpected signing algorithm encountered: " + signingAlgorithm.getName());
        }
    }

    private boolean verifySignature(SignedJWT signedJWT, PublicKey clientPublicKey)
            throws JOSEException, ClientConfigurationException {
        if (clientPublicKey instanceof RSAPublicKey) {
            RSASSAVerifier rsassaVerifier = new RSASSAVerifier((RSAPublicKey) clientPublicKey);
            return signedJWT.verify(rsassaVerifier);
        } else if (clientPublicKey instanceof ECPublicKey) {
            ECDSAVerifier ecdsaVerifier = new ECDSAVerifier((ECPublicKey) clientPublicKey);
            return signedJWT.verify(ecdsaVerifier);
        } else {
            throw new ClientConfigurationException(
                    new IllegalStateException(
                            "unknown public signing key: " + clientPublicKey.getAlgorithm()));
        }
    }
}
