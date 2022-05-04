package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.Header;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWKSetHandlerTest {

    @Mock JWKSetService jwkSetService;

    @Test
    void shouldAddJWKsToAJWKSetAndReturn() throws JOSEException, ParseException {
        JWK sampleJWK = createRSAJWK();
        when(jwkSetService.getJWKs()).thenReturn(List.of(sampleJWK));
        APIGatewayProxyResponseEvent responseEvent =
                new JWKSetHandler(jwkSetService).handleRequest(null, null);
        assertEquals(HttpStatus.SC_OK, responseEvent.getStatusCode());
        assertEquals(JWKSet.MIME_TYPE, responseEvent.getHeaders().get(Header.CONTENT_TYPE));
        JWKSet jwkSet = JWKSet.parse(responseEvent.getBody());
        assertTrue(jwkSet.containsJWK(sampleJWK));
    }

    private JWK createRSAJWK() throws JOSEException {
        RSAKey jwk =
                new RSAKeyGenerator(2048)
                        .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                        .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                        .generate();
        return jwk.toPublicJWK();
    }
}
