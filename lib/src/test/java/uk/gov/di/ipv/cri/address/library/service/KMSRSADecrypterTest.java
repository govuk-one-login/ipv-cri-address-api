package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptionAlgorithmSpec;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KMSRSADecrypterTest {
    private static final String TEST_KEY_ID = "test-key";
    @Mock private AWSKMS mockKmsClient;
    private KMSRSADecrypter kmsRsaDecrypter;

    @BeforeEach
    void setup() {
        this.kmsRsaDecrypter = new KMSRSADecrypter(TEST_KEY_ID, this.mockKmsClient);
    }

    @Test
    void shouldDecrypt() throws ParseException, JOSEException {
        JWEHeader header = createHeader();
        Base64URL encryptedKey =
                Base64URL.from(
                        "jfDZSCq6Z7Hu22uWaNEtDfFfv-RZot58oxhTAwNoGT3aMvWUiZBIzqm0b9f2xkxMBEky3oix9xC5_KRL2Xv-OO9DdTw7sfLMUs7BidEXWRIAq7PgiD1rdkQ5ElZHM1TPYoREXhJyqtXMgup8lD_B85m-xBOgaZQvuG_cxc0lNerLBgu1f23jcy0S8G3P8L-Cl056Kv6QV-WGFOQW0Vurwd_f432Ho1W1STYrSat22YNkX2_A0SJZGVcxF_wKKfNAUw4n7sVdYZOfl62x7Cz2Rt2HX36U6vLhI8ZLNGROCsNKI-LYJA2ET1_li150DMgMNlfYfwHrO3jFi_j1XcK_oA");
        Base64URL iv = Base64URL.from("esSJbN3jlduupMFy");
        Base64URL cipherText =
                Base64URL.from(
                        "cT7gnhBT0VT7jY5gEAsafuZi-o6BP8DI-aaH97mJ4e6q0E1pAgWkWAHc-qvmRWYHLUfbMlTOpH5AlQNhQ-ZWsfm40eM0sIV3OZCk4KcAbSoz4v-9aqleBTVhr_YhZqk_lZ9I9566SzLnOuPkWQr6J5F6F19Ol7Ob0j7-a2zHgXlxQizp1hjXiWAhJ0aFFRfP4hxcohn7h5EKeMw8ZT8jv1kqc0PwRoZOt83SgBcdlLcIz9LDPIUWuXXtw9Xi5FrfAc2SXFv4sv7BEo70-ICT9sC1jTpkMsqJlofqu5R3L2Kf51HFOJe2C1SRy_MQGID9FnQGgrDburfSpcmH_DPxdLS8SJ9X7LyyrPWzrdTwgUDdUCWmsoYbvgZQC1KhRiu7GjKLDU2uQgo0NSiaNIcyS6qllDXPqJUTkz0snmMUjcIN7ZTzA29ngxJh5OhI444qChQrB-2hU769giX00UEyqb--MpTWybGReoC0nF-BzaZrrQkWMB2vFWiDg5dUUD6778b4YvmryINCP5H4NteK8JHnIsqMMbY6wxtZFqVhsvVAR6thM9JBKJrN5nSMkKlwSAEpf2vbUyec2x_AZQ6d66lrneZe3VHWmHAo42d6if2P-yaL2vLrr9g73vr7CfU9WiTYTYtFOJ0aWodFwnSeZq-Bek1RXTNsEl4G8K3ved97W1YlEW4359V6OWpSCfFouDJv-yLxaedRvzXjcBH0Ssx6D8Njs4cOduQ-PE22mUcpHd5URsUsU19F59jgXpk");
        Base64URL authTag = Base64URL.from("I48OP5ZO-bl9nqunO4VX6w");
        DecryptResult decryptResult = new DecryptResult();

        decryptResult.setPlaintext(
                ByteBuffer.wrap(
                        Base64.getDecoder()
                                .decode("ngoABokVaj3BYY8FfaPef4nzV9dr+ziueibf2hofYDQ=")));
        when(mockKmsClient.decrypt(any(DecryptRequest.class))).thenReturn(decryptResult);

        byte[] result = this.kmsRsaDecrypter.decrypt(header, encryptedKey, iv, cipherText, authTag);
        SignedJWT signedJWT = SignedJWT.parse(new String(result, StandardCharsets.UTF_8));
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        ArgumentCaptor<DecryptRequest> decryptRequestArgumentCaptor =
                ArgumentCaptor.forClass(DecryptRequest.class);
        verify(mockKmsClient).decrypt(decryptRequestArgumentCaptor.capture());
        DecryptRequest actualDecryptRequest = decryptRequestArgumentCaptor.getValue();
        assertEquals(
                EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256.toString(),
                actualDecryptRequest.getEncryptionAlgorithm());
        assertEquals(TEST_KEY_ID, actualDecryptRequest.getKeyId());
        assertEquals(
                ByteBuffer.wrap(encryptedKey.decode()), actualDecryptRequest.getCiphertextBlob());
        assertEquals(11, claims.getClaims().size());
        assertEquals("urn:uuid:8d097496-4410-49db-acdb-ffdca993fd2f", claims.getSubject());
        assertEquals("ipv-core-stub", claims.getIssuer());
    }

    @Test
    void shouldThrowExceptionWhenEncryptedKeyIsNull() throws ParseException {
        JWEHeader header = createHeader();
        Base64URL testBase64URL = Base64URL.from("esS");
        assertThrows(
                JOSEException.class,
                () ->
                        this.kmsRsaDecrypter.decrypt(
                                header, null, testBase64URL, testBase64URL, testBase64URL),
                "Missing JWE encrypted key");
    }

    @Test
    void shouldThrowExceptionWhenInitVectorIsNull() throws ParseException {
        JWEHeader header = createHeader();
        Base64URL testBase64URL = Base64URL.from("esS");
        assertThrows(
                JOSEException.class,
                () ->
                        this.kmsRsaDecrypter.decrypt(
                                header, testBase64URL, null, testBase64URL, testBase64URL),
                "Missing JWE initialization vector (IV)");
    }

    @Test
    void shouldThrowExceptionWhenAuthTagIsNull() throws ParseException {
        JWEHeader header = createHeader();
        Base64URL testBase64URL = Base64URL.from("esS");
        assertThrows(
                JOSEException.class,
                () ->
                        this.kmsRsaDecrypter.decrypt(
                                header, testBase64URL, testBase64URL, testBase64URL, null),
                "Missing JWE authentication tag");
    }

    @Test
    void shouldThrowExceptionWhenUnsupportedAlgorithmSupplied() throws ParseException {
        JWEHeader header = createHeader(JWEAlgorithm.ECDH_1PU_A256KW);
        Base64URL testBase64URL = Base64URL.from("esS");
        assertThrows(
                JOSEException.class,
                () ->
                        this.kmsRsaDecrypter.decrypt(
                                header, testBase64URL, testBase64URL, testBase64URL, testBase64URL),
                "Unsupported JWE algorithm ECDH-1PU+A256KW, must be RSA-OAEP-256");
    }

    private JWEHeader createHeader() throws ParseException {
        return createHeader(JWEAlgorithm.RSA_OAEP_256);
    }

    private JWEHeader createHeader(JWEAlgorithm jweAlgorithm) throws ParseException {
        return JWEHeader.parse(
                "{\"cty\":\"JWT\", \"enc\":\"A256GCM\", \"alg\":\""
                        + jweAlgorithm.getName()
                        + "\"}");
    }
}
