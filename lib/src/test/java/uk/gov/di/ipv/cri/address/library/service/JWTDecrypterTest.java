package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWTDecrypterTest {
    @Mock private KMSRSADecrypter mockDecrypter;
    private JWTDecrypter jwtDecrypter;

    @BeforeEach
    void setup() {
        this.jwtDecrypter = new JWTDecrypter(mockDecrypter);
    }

    @Test
    void shouldDecrypt() throws ParseException, JOSEException {
        String encryptedJWT =
                "eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAtMjU2In0.jfDZSCq6Z7Hu22uWaNEtDfFfv-RZot58oxhTAwNoGT3aMvWUiZBIzqm0b9f2xkxMBEky3oix9xC5_KRL2Xv-OO9DdTw7sfLMUs7BidEXWRIAq7PgiD1rdkQ5ElZHM1TPYoREXhJyqtXMgup8lD_B85m-xBOgaZQvuG_cxc0lNerLBgu1f23jcy0S8G3P8L-Cl056Kv6QV-WGFOQW0Vurwd_f432Ho1W1STYrSat22YNkX2_A0SJZGVcxF_wKKfNAUw4n7sVdYZOfl62x7Cz2Rt2HX36U6vLhI8ZLNGROCsNKI-LYJA2ET1_li150DMgMNlfYfwHrO3jFi_j1XcK_oA.esSJbN3jlduupMFy.cT7gnhBT0VT7jY5gEAsafuZi-o6BP8DI-aaH97mJ4e6q0E1pAgWkWAHc-qvmRWYHLUfbMlTOpH5AlQNhQ-ZWsfm40eM0sIV3OZCk4KcAbSoz4v-9aqleBTVhr_YhZqk_lZ9I9566SzLnOuPkWQr6J5F6F19Ol7Ob0j7-a2zHgXlxQizp1hjXiWAhJ0aFFRfP4hxcohn7h5EKeMw8ZT8jv1kqc0PwRoZOt83SgBcdlLcIz9LDPIUWuXXtw9Xi5FrfAc2SXFv4sv7BEo70-ICT9sC1jTpkMsqJlofqu5R3L2Kf51HFOJe2C1SRy_MQGID9FnQGgrDburfSpcmH_DPxdLS8SJ9X7LyyrPWzrdTwgUDdUCWmsoYbvgZQC1KhRiu7GjKLDU2uQgo0NSiaNIcyS6qllDXPqJUTkz0snmMUjcIN7ZTzA29ngxJh5OhI444qChQrB-2hU769giX00UEyqb--MpTWybGReoC0nF-BzaZrrQkWMB2vFWiDg5dUUD6778b4YvmryINCP5H4NteK8JHnIsqMMbY6wxtZFqVhsvVAR6thM9JBKJrN5nSMkKlwSAEpf2vbUyec2x_AZQ6d66lrneZe3VHWmHAo42d6if2P-yaL2vLrr9g73vr7CfU9WiTYTYtFOJ0aWodFwnSeZq-Bek1RXTNsEl4G8K3ved97W1YlEW4359V6OWpSCfFouDJv-yLxaedRvzXjcBH0Ssx6D8Njs4cOduQ-PE22mUcpHd5URsUsU19F59jgXpk.I48OP5ZO-bl9nqunO4VX6w";
        byte[] decryptedJWT =
                new SignedJWTBuilder().build().serialize().getBytes(StandardCharsets.UTF_8);
        when(mockDecrypter.decrypt(
                        any(JWEHeader.class),
                        any(Base64URL.class),
                        any(Base64URL.class),
                        any(Base64URL.class),
                        any(Base64URL.class)))
                .thenReturn(decryptedJWT);

        SignedJWT decryptedSignedJWT = this.jwtDecrypter.decrypt(encryptedJWT);

        ArgumentCaptor<JWEHeader> headerArgumentCaptor = ArgumentCaptor.forClass(JWEHeader.class);
        verify(mockDecrypter)
                .decrypt(
                        headerArgumentCaptor.capture(),
                        any(Base64URL.class),
                        any(Base64URL.class),
                        any(Base64URL.class),
                        any(Base64URL.class));
        JWEHeader actualHeader = headerArgumentCaptor.getValue();
        assertEquals(JWEAlgorithm.RSA_OAEP_256.getName(), actualHeader.getAlgorithm().getName());
        assertEquals(
                EncryptionMethod.A256GCM.getName(), actualHeader.getEncryptionMethod().getName());
        assertEquals("JWT", actualHeader.getContentType());
        assertNotNull(decryptedSignedJWT);
    }
}
