package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.helpers.SignClaimSetJwt;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;

import java.util.List;
import java.util.Map;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.ADDRESS_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.CREDENTIAL_SUBJECT_ADDRESS;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.DI_CONTEXT;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_ADDRESS_KEY;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_CONTEXT;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_CREDENTIAL_SUBJECT;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_TYPE;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VERIFIABLE_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.W3_BASE_CONTEXT;

@ExtendWith(MockitoExtension.class)
public class VerifiableCredentialServiceTest {
    @Test
    void shouldReturnAVerifiedCredentialWhenGivenCanonicalAddresses() throws JOSEException {

        SignClaimSetJwt mockSignedClaimSetJwt = mock(SignClaimSetJwt.class);
        ConfigurationService mockConfigurationService = mock(ConfigurationService.class);
        var verifiableCredentialService =
                new VerifiableCredentialService(mockSignedClaimSetJwt, mockConfigurationService);

        when(mockConfigurationService.getVerifiableCredentialIssuer())
                .thenReturn("address-cri-issue");
        when(mockConfigurationService.getMaxJwtTtl()).thenReturn(342L);

        var mockCanonicalAddressList = List.of(mock(CanonicalAddress.class));

        verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                "subject", mockCanonicalAddressList);
        System.out.println(
                createVerifiableCredentialStructure(mockCanonicalAddressList).toPrettyString());
        verify(mockSignedClaimSetJwt).createSignedJwt(any());
    }

    ObjectNode createVerifiableCredentialStructure(List<CanonicalAddress> canonicalAddresses) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode verifiableCredentialStructure = objectMapper.createObjectNode();
        ObjectNode context = objectMapper.createObjectNode();
        ArrayNode contextValues = objectMapper.createArrayNode();
        ArrayNode types = objectMapper.createArrayNode();
        ArrayNode addressStructure = objectMapper.createArrayNode();
        ObjectNode credentials = objectMapper.createObjectNode();
        ObjectNode addresses = objectMapper.createObjectNode();

        contextValues.add(W3_BASE_CONTEXT).add(DI_CONTEXT);
        types.add(VERIFIABLE_CREDENTIAL_TYPE).add(ADDRESS_CREDENTIAL_TYPE);
        addressStructure.add(VC_CREDENTIAL_SUBJECT);
        addresses.putPOJO(CREDENTIAL_SUBJECT_ADDRESS, canonicalAddresses);

        credentials.putArray(VC_ADDRESS_KEY);
        return verifiableCredentialStructure
                .put(SUBJECT, "urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
                .put(NOT_BEFORE, "1647017990")
                .put(ISSUER, "https://address-cri.account.gov.uk.TBC")
                .put(EXPIRATION_TIME, "1647017990")
                .set(
                        VC_CLAIM,
                        context.setAll(
                                Map.of(
                                        VC_CONTEXT,
                                        contextValues,
                                        VC_TYPE,
                                        types,
                                        VC_CREDENTIAL_SUBJECT,
                                        credentials)));
    }
}
