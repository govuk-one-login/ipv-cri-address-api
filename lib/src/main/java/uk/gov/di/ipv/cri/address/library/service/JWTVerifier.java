package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
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

public class JWTVerifier {

    public void verifyJWT(
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
            PublicKey pubicKeyFromConfig = getPublicKeyFromConfig(publicCertificateToVerify);

            if (!validSignature(signedJWT, pubicKeyFromConfig)) {
                throw new SessionValidationException("JWT signature verification failed");
            }
        } catch (JOSEException e) {
            throw new SessionValidationException("JWT signature verification failed", e);
        } catch (CertificateException e) {
            throw new ClientConfigurationException(e);
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
            throw new SessionValidationException(e.getMessage());
        }
    }

    private PublicKey getPublicKeyFromConfig(String base64) throws CertificateException {
        byte[] binaryCertificate = Base64.getDecoder().decode(base64);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        Certificate certificate =
                factory.generateCertificate(new ByteArrayInputStream(binaryCertificate));
        return certificate.getPublicKey();
    }

    private boolean validSignature(SignedJWT signedJWT, PublicKey clientPublicKey)
            throws JOSEException, ClientConfigurationException {
        if (clientPublicKey instanceof RSAPublicKey) {
            RSASSAVerifier rsassaVerifier = new RSASSAVerifier((RSAPublicKey) clientPublicKey);
            return signedJWT.verify(rsassaVerifier);
        } else if (clientPublicKey instanceof ECPublicKey) {
            ECDSAVerifier ecdsaVerifier = new ECDSAVerifier((ECPublicKey) clientPublicKey);
            return signedJWT.verify(ecdsaVerifier);
        } else {
            throw new ClientConfigurationException(
                    new IllegalStateException("unknown public JWT signing key"));
        }
    }
}
