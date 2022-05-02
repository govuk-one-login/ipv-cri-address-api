package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.GetPublicKeyRequest;
import com.amazonaws.services.kms.model.GetPublicKeyResult;
import com.amazonaws.services.kms.model.KeyUsageType;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KMSServiceTest {

    @Mock private GetPublicKeyResult publicKeyResult;

    @Mock private AWSKMSClient kmsClient;

    @Test
    void generateRSAJWKFromRSAPublicKey() throws JOSEException {
        String keyId = "a key id";
        GetPublicKeyRequest publicKeyRequest = new GetPublicKeyRequest();
        publicKeyRequest.setKeyId(keyId);
        when(kmsClient.getPublicKey(publicKeyRequest)).thenReturn(publicKeyResult);
        when(publicKeyResult.getPublicKey())
                .thenReturn(ByteBuffer.wrap(generateRSAPublicKey().getEncoded()));
        when(publicKeyResult.getKeyUsage()).thenReturn(KeyUsageType.ENCRYPT_DECRYPT.toString());
        when(publicKeyResult.getKeySpec()).thenReturn("RSA");
        JWK jwk = new KMSService(kmsClient).getJWK(keyId);
        assertEquals(KeyType.RSA, jwk.getKeyType());
        assertEquals(keyId, jwk.getKeyID());
    }

    @Test
    void generateECJWKFromECPublicKey() throws JOSEException {
        String keyId = "a key id";
        GetPublicKeyRequest publicKeyRequest = new GetPublicKeyRequest();
        publicKeyRequest.setKeyId(keyId);
        when(kmsClient.getPublicKey(publicKeyRequest)).thenReturn(publicKeyResult);
        when(publicKeyResult.getPublicKey())
                .thenReturn(ByteBuffer.wrap(generateECPublicKey().getEncoded()));
        when(publicKeyResult.getKeyUsage()).thenReturn(KeyUsageType.SIGN_VERIFY.toString());
        when(publicKeyResult.getKeySpec()).thenReturn("EC");
        JWK jwk = new KMSService(kmsClient).getJWK(keyId);
        assertEquals(KeyType.EC, jwk.getKeyType());
        assertEquals(keyId, jwk.getKeyID());
    }

    private PublicKey generateRSAPublicKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate()
                .toPublicKey();
    }

    public PublicKey generateECPublicKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate()
                .toPublicKey();
    }
}
