package uk.gov.di.ipv.cri.kbv.acceptancetest.api;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;

public class JWTSigner {

    private final JWSSigner jwsSigner;
    private final String keyId;

    JWTSigner(ECKey ecSigningKey) throws JOSEException {
        this.keyId = ecSigningKey.getKeyID();
        this.jwsSigner = new ECDSASigner(ecSigningKey);
    }

    void signJWT(SignedJWT jwtToSign) throws JOSEException {
        jwtToSign.sign(this.jwsSigner);
    }

    String getKeyId() {
        return keyId;
    }
}
