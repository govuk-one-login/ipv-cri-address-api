package uk.gov.di.ipv.cri.address.library.helpers;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class SignClaimSetJwt {
    private final JWSSigner kmsSigner;

    public SignClaimSetJwt(JWSSigner kmsSigner) {
        this.kmsSigner = kmsSigner;
    }

    public SignedJWT createSignedJwt(JWTClaimsSet claimsSet) throws JOSEException {
        JWSHeader jwsHeader = generateHeader();
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(kmsSigner);
        return signedJWT;
    }

    private JWSHeader generateHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    }
}
