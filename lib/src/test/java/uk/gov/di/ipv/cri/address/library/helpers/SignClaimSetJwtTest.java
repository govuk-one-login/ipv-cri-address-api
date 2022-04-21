package uk.gov.di.ipv.cri.address.library.helpers;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.helpers.fixtures.TestFixtures;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(MockitoExtension.class)
class SignClaimSetJwtTest implements TestFixtures {

    private SignClaimSetJwt signClaimSetJwt;

    @Test
    void shouldCreateASignedJwtSuccessfully()
            throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException,
                    ParseException {
        JWTClaimsSet testClaimsSet = new JWTClaimsSet.Builder().build();
        signClaimSetJwt = new SignClaimSetJwt(new ECDSASigner(getPrivateKey()));

        SignedJWT signedJWT = signClaimSetJwt.createSignedJwt(testClaimsSet);

        assertThat(signedJWT.verify(new ECDSAVerifier(ECKey.parse(EC_PUBLIC_JWK_1))), is(true));
    }
}
