package uk.gov.di.ipv.cri.address.library.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Map;

public class SignClaimSetJwt {
    private final JWSSigner kmsSigner;
    private static final ObjectMapper mapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    public SignClaimSetJwt(JWSSigner kmsSigner) {
        this.kmsSigner = kmsSigner;
    }

    public <T> SignedJWT createSignedJwt(T claimInput) throws JOSEException {
        JWSHeader jwsHeader = generateHeader();
        JWTClaimsSet claimsSet = generateClaims(claimInput);
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(kmsSigner);
        return signedJWT;
    }

    private JWSHeader generateHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    }

    private <T> JWTClaimsSet generateClaims(T claimInput) {
        var claimsBuilder = new JWTClaimsSet.Builder();

        mapper.convertValue(claimInput, Map.class)
                .forEach((key, value) -> claimsBuilder.claim((String) key, value));

        return claimsBuilder.build();
    }
}
