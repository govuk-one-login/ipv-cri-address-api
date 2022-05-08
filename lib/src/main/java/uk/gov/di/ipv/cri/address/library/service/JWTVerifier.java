package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.oauth2.sdk.id.ClientID;
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
import java.util.Map;
import java.util.Set;

import static com.nimbusds.jose.JWSAlgorithm.ES256;

public class JWTVerifier {

    public void verifyAuthorizationJWT(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT)
            throws SessionValidationException, ClientConfigurationException {
        verifyJWT(
                clientAuthenticationConfig,
                signedJWT,
                Set.of(
                        JWTClaimNames.EXPIRATION_TIME,
                        JWTClaimNames.SUBJECT,
                        JWTClaimNames.NOT_BEFORE),
                new JWTClaimsSet.Builder()
                        .issuer(clientAuthenticationConfig.get("issuer"))
                        .audience(clientAuthenticationConfig.get("audience"))
                        .build());
    }

    public void verifyAccessTokenJWT(
            Map<String, String> clientAuthenticationConfig, SignedJWT signedJWT, ClientID clientID)
            throws SessionValidationException, ClientConfigurationException {
        Set<String> requiredClaims =
                Set.of(
                        JWTClaimNames.EXPIRATION_TIME,
                        JWTClaimNames.SUBJECT,
                        JWTClaimNames.ISSUER,
                        JWTClaimNames.AUDIENCE,
                        JWTClaimNames.JWT_ID);
        JWTClaimsSet expectedClaimValues =
                new JWTClaimsSet.Builder()
                        .issuer(clientID.getValue())
                        .subject(clientID.getValue())
                        .audience(clientAuthenticationConfig.get("audience"))
                        .build();
        verifyJWT(clientAuthenticationConfig, signedJWT, requiredClaims, expectedClaimValues);
    }

    private void verifyJWT(
            Map<String, String> clientAuthenticationConfig,
            SignedJWT signedJWT,
            Set<String> requiredClaims,
            JWTClaimsSet expectedClaimValues)
            throws SessionValidationException, ClientConfigurationException {
        this.verifyJWTHeader(clientAuthenticationConfig, signedJWT);
        this.verifyJWTClaimsSet(signedJWT, requiredClaims, expectedClaimValues);
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
        String publicCertificateToVerify = clientAuthenticationConfig.get("publicSigningJwkBase64");
        try {

            SignedJWT concatSignatureJwt;
            if (signatureIsDerFormat(signedJWT)) {
                concatSignatureJwt = transcodeSignature(signedJWT);
            } else {
                concatSignatureJwt = signedJWT;
            }
            JWSAlgorithm signingAlgorithm = signedJWT.getHeader().getAlgorithm();
            PublicKey pubicKeyFromConfig =
                    getPublicKeyFromConfig(publicCertificateToVerify, signingAlgorithm);
            if (!verifySignature(concatSignatureJwt, pubicKeyFromConfig)) {
                throw new SessionValidationException("JWT signature verification failed");
            }
        } catch (JOSEException | ParseException e) {
            throw new SessionValidationException("JWT signature verification failed", e);
        } catch (CertificateException e) {
            throw new ClientConfigurationException("Certificate problem encountered", e);
        }
    }

    private boolean signatureIsDerFormat(SignedJWT signedJWT) throws JOSEException {
        return signedJWT.getSignature().decode().length != ECDSA.getSignatureByteArrayLength(ES256);
    }

    private SignedJWT transcodeSignature(SignedJWT signedJWT) throws JOSEException, ParseException {
        Base64URL transcodedSignatureBase64 =
                Base64URL.encode(
                        ECDSA.transcodeSignatureToConcat(
                                signedJWT.getSignature().decode(),
                                ECDSA.getSignatureByteArrayLength(ES256)));
        String[] jwtParts = signedJWT.serialize().split("\\.");
        return SignedJWT.parse(
                String.format("%s.%s.%s", jwtParts[0], jwtParts[1], transcodedSignatureBase64));
    }

    private void verifyJWTClaimsSet(
            SignedJWT signedJWT, Set<String> requiredClaims, JWTClaimsSet expectedClaimValues)
            throws SessionValidationException {

        try {
            new DefaultJWTClaimsVerifier<>(expectedClaimValues, requiredClaims)
                    .verify(signedJWT.getJWTClaimsSet(), null);
        } catch (BadJWTException | ParseException e) {
            throw new SessionValidationException(e.getMessage());
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
            return ECKey.parse(new String(Base64.getDecoder().decode(serialisedPublicKey)))
                    .toECPublicKey();
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
