package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

class JWTDecrypter {
    private final KMSRSADecrypter decrypter;

    JWTDecrypter(KMSRSADecrypter decrypter) {
        this.decrypter = decrypter;
    }

    SignedJWT decrypt(String serialisedJweObj) throws ParseException, JOSEException {
        JWEObject requestJweObj = JWEObject.parse(serialisedJweObj);
        requestJweObj.decrypt(this.decrypter);
        return requestJweObj.getPayload().toSignedJWT();
    }
}
