package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.Tag;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWKSetHandlerTest {

    @Mock private KMSService kmsService;

    @Mock private KeyListEntry keyListEntry;

    @Mock private KeyMetadata keyMetadata;

    @Mock private Tag tag;

    @Test
    void shouldFilterToFindTaggedAndEnabledRSAKMSKey() throws JOSEException {
        String keyId = "a kms key id";
        when(keyListEntry.getKeyId()).thenReturn(keyId);
        when(kmsService.getKeys()).thenReturn(List.of(keyListEntry));
        when(kmsService.getTags(keyId)).thenReturn(List.of(tag));
        when(tag.getTagKey()).thenReturn("jwkset");
        when(kmsService.getMetadata(keyId)).thenReturn(keyMetadata);
        when(keyMetadata.getKeyState()).thenReturn("Enabled");
        when(kmsService.getJWK(keyId)).thenReturn(createRSAJWK());

        APIGatewayProxyResponseEvent responseEvent =
                new JWKSetHandler(kmsService).handleRequest(null, null);
        assertEquals(HttpStatus.SC_OK, responseEvent.getStatusCode());
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
