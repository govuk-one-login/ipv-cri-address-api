package uk.gov.di.ipv.cri.address.library.helpers;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.MessageType;
import com.amazonaws.services.kms.model.SignRequest;
import com.amazonaws.services.kms.model.SignResult;
import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.shaded.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KMSSignerTest {
    private KMSSigner kmsSigner;
    private String kid = UUID.randomUUID().toString();
    @Mock private AWSKMS mockKmsClient;

    @BeforeEach
    void setUp() {
        kmsSigner = new KMSSigner(kid, mockKmsClient);
    }

    @Test
    void shouldCreateKMSSignerSuccessfully() {
        assertThat(kmsSigner, notNullValue());
        assertThat(kmsSigner.getJCAContext(), notNullValue());
        assertThat(Set.of(JWSAlgorithm.ES256), equalTo(kmsSigner.supportedJWSAlgorithms()));
    }

    @Test
    void shouldSignJWSHeaderSuccessfully() throws JOSEException {
        JWSHeader mockJWSHeader = mock(JWSHeader.class);
        byte[] data = new byte[0];
        ArgumentCaptor<SignRequest> signRequestArgumentCaptor =
                ArgumentCaptor.forClass(SignRequest.class);

        SignResult mockSignResult = mock(SignResult.class);
        ByteBuffer mockByteBuffer = mock(ByteBuffer.class);

        when(mockKmsClient.sign(signRequestArgumentCaptor.capture())).thenReturn(mockSignResult);
        when(mockSignResult.getSignature()).thenReturn(mockByteBuffer);
        when(mockByteBuffer.array()).thenReturn(data);

        var signed = kmsSigner.sign(mockJWSHeader, data);

        verify(mockKmsClient).sign(signRequestArgumentCaptor.capture());
        SignRequest capturedSignRequest = signRequestArgumentCaptor.getValue();
        assertThat(signed, notNullValue());
        assertThat(capturedSignRequest.getKeyId(), equalTo(kid));
        assertThat(capturedSignRequest.getMessageType(), equalTo(MessageType.DIGEST.toString()));
        assertThat(
                capturedSignRequest.getSigningAlgorithm(),
                equalTo(SigningAlgorithmSpec.ECDSA_SHA_256.toString()));
    }

    @Test
    void shouldSignJWSObject() throws JOSEException {
        var signResult = mock(SignResult.class);
        when(mockKmsClient.sign(any(SignRequest.class))).thenReturn(signResult);

        byte[] bytes = new byte[10];
        when(signResult.getSignature()).thenReturn(ByteBuffer.wrap(bytes));

        JSONObject jsonPayload = new JSONObject(Map.of("test", "test"));

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        JWSObject jwsObject = new JWSObject(jwsHeader, new Payload(jsonPayload));

        jwsObject.sign(kmsSigner);

        assertEquals(JWSObject.State.SIGNED, jwsObject.getState());
        assertEquals(jwsHeader, jwsObject.getHeader());
        assertEquals(jsonPayload.toJSONString(), jwsObject.getPayload().toString());
    }

    @Test
    void shouldThrowNoSuchAlgorithmExceptionWhenSigningJWSHeaderWithADifferentAlgorithm() {
        JWSHeader mockJWSHeader = mock(JWSHeader.class);

        var exception =
                assertThrows(NullPointerException.class, () -> kmsSigner.sign(mockJWSHeader, null));

        assertThat(exception.getMessage(), containsString("Signing input must not be null"));
    }
}
