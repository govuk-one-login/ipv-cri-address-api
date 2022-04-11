package uk.gov.di.ipv.cri.address.library.helpers;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.nimbusds.jose.JWSAlgorithm.ES256;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignClaimSetJwtTest {
    @Mock private KMSSigner kmsSigner;
    @InjectMocks private SignClaimSetJwt signClaimSetJwt;

    @Test
    void shouldCreateASignedJwtSuccessfully() throws JOSEException {
        ClaimsSet mockClaimSet = mock(ClaimsSet.class);

        when(kmsSigner.supportedJWSAlgorithms()).thenReturn(Set.of(ES256));
        SignedJWT signedJWT = signClaimSetJwt.createSignedJwt(mockClaimSet);

        assertThat(signedJWT, notNullValue());
    }
}
