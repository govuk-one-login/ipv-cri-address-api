package uk.gov.di.ipv.cri.address.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.api.service.fixtures.TestFixtures;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.ADDRESS_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.DI_CONTEXT;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.W3_BASE_CONTEXT;
import static uk.gov.di.ipv.cri.address.api.objectmapper.CustomObjectMapper.getMapperWithCustomSerializers;

@ExtendWith(MockitoExtension.class)
class VerifiableCredentialServiceTest implements TestFixtures {
    private static final JWTClaimsSet TEST_CLAIMS_SET =
            new JWTClaimsSet.Builder().subject("test").issuer("test").build();
    private static final long DEFAULT_JWT_TTL = 6L;
    private static final String DEFAULT_JWT_TTL_UNIT = "MONTHS";
    private static final String VC_ISSUER = "vc-issuer";
    private static final String SUBJECT = "subject";
    private final ObjectMapper objectMapper = getMapperWithCustomSerializers();
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private SignedJWTFactory mockSignedClaimSetJwt;
    @Mock private VerifiableCredentialClaimsSetBuilder mockVcClaimSetBuilder;
    @Captor private ArgumentCaptor<Map<String, Object>> mapArgumentCaptor;

    private VerifiableCredentialService verifiableCredentialService;

    @BeforeEach
    void setUp() {
        this.verifiableCredentialService =
                new VerifiableCredentialService(
                        mockSignedClaimSetJwt,
                        mockConfigurationService,
                        objectMapper,
                        mockVcClaimSetBuilder);
    }

    @Test
    void shouldReturnAVerifiedCredentialWhenGivenCanonicalAddresses()
            throws JOSEException, ParseException, JsonProcessingException {
        initMockConfigurationService();
        initMockVCClaimSetBuilder();
        when(mockVcClaimSetBuilder.build()).thenReturn(TEST_CLAIMS_SET);

        var canonicalAddresses = List.of(mock(CanonicalAddress.class));

        verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                SUBJECT, canonicalAddresses);

        verify(mockSignedClaimSetJwt)
                .createSignedJwt(objectMapper.writeValueAsString(TEST_CLAIMS_SET));
    }

    @Test
    void shouldCreateValidSignedJWT()
            throws InvalidKeySpecException, NoSuchAlgorithmException, JOSEException, ParseException,
                    JsonProcessingException {
        initMockConfigurationService();
        initMockVCClaimSetBuilder();
        when(mockVcClaimSetBuilder.build()).thenReturn(TEST_CLAIMS_SET);

        CanonicalAddress address = new CanonicalAddress();
        address.setUprn(72262801L);
        address.setBuildingNumber("8");
        address.setStreetName("GRANGE FIELDS WAY");
        address.setAddressLocality("LEEDS");
        address.setPostalCode("LS10 4QL");
        address.setAddressCountry("GB");
        address.setValidFrom(LocalDate.of(2010, 2, 26));
        address.setValidUntil(LocalDate.of(2021, 1, 16));
        List<CanonicalAddress> canonicalAddresses = List.of(address);

        SignedJWTFactory signedJwtFactory = new SignedJWTFactory(new ECDSASigner(getPrivateKey()));
        verifiableCredentialService =
                new VerifiableCredentialService(
                        signedJwtFactory,
                        mockConfigurationService,
                        objectMapper,
                        mockVcClaimSetBuilder);

        SignedJWT signedJWT =
                verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        SUBJECT, canonicalAddresses);
        assertTrue(signedJWT.verify(new ECDSAVerifier(ECKey.parse(TestFixtures.EC_PUBLIC_JWK_1))));

        verify(mockVcClaimSetBuilder).build();
        verify(mockVcClaimSetBuilder).subject(SUBJECT);
        verify(mockVcClaimSetBuilder).verifiableCredentialType(ADDRESS_CREDENTIAL_TYPE);
        verify(mockVcClaimSetBuilder)
                .timeToLive(DEFAULT_JWT_TTL, ChronoUnit.valueOf(DEFAULT_JWT_TTL_UNIT));
        verify(mockVcClaimSetBuilder)
                .verifiableCredentialContext(new String[] {W3_BASE_CONTEXT, DI_CONTEXT});
        verify(mockVcClaimSetBuilder).verifiableCredentialSubject(mapArgumentCaptor.capture());
        Map<?, ?> vcSubjectClaims =
                (Map<?, ?>) ((Object[]) mapArgumentCaptor.getValue().get("address"))[0];
        assertAll(
                () -> {
                    assertEquals(address.getUprn(), vcSubjectClaims.get("uprn"));
                    assertEquals(
                            address.getBuildingNumber(), vcSubjectClaims.get("buildingNumber"));
                    assertEquals(address.getStreetName(), vcSubjectClaims.get("streetName"));
                    assertEquals(
                            address.getAddressLocality(), vcSubjectClaims.get("addressLocality"));
                    assertEquals(address.getPostalCode(), vcSubjectClaims.get("postalCode"));
                    assertEquals(
                            address.getAddressCountry(), vcSubjectClaims.get("addressCountry"));
                    assertEquals(
                            address.getValidFrom().toString(), vcSubjectClaims.get("validFrom"));
                    assertEquals(
                            address.getValidUntil().toString(), vcSubjectClaims.get("validUntil"));
                });
    }

    @Test
    void shouldGetAuditEventContext() {
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(VC_ISSUER);

        Map<String, Object> auditEventContext =
                verifiableCredentialService.getAuditEventExtensions(
                        List.of(new CanonicalAddress()));

        assertEquals(VC_ISSUER, auditEventContext.get("iss"));
        assertEquals(1, auditEventContext.get("addressesEntered"));
    }

    @Test
    void shouldGetAuditEventContextWithZeroAddressesEnteredWhenNullProvided() {
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("vc-issuer");

        Map<String, Object> auditEventContext =
                verifiableCredentialService.getAuditEventExtensions(null);
        assertEquals(0, auditEventContext.get("addressesEntered"));
    }

    private void initMockVCClaimSetBuilder() {
        when(mockVcClaimSetBuilder.subject(anyString())).thenReturn(mockVcClaimSetBuilder);
        when(mockVcClaimSetBuilder.timeToLive(anyLong(), any(ChronoUnit.class)))
                .thenReturn(mockVcClaimSetBuilder);
        when(mockVcClaimSetBuilder.verifiableCredentialContext(any(String[].class)))
                .thenReturn(mockVcClaimSetBuilder);
        when(mockVcClaimSetBuilder.verifiableCredentialSubject(any()))
                .thenReturn(mockVcClaimSetBuilder);
        when(mockVcClaimSetBuilder.verifiableCredentialType(ADDRESS_CREDENTIAL_TYPE))
                .thenReturn(mockVcClaimSetBuilder);
    }

    private void initMockConfigurationService() {
        when(mockConfigurationService.getMaxJwtTtl()).thenReturn(DEFAULT_JWT_TTL);
        when(mockConfigurationService.getParameterValue("JwtTtlUnit"))
                .thenReturn(DEFAULT_JWT_TTL_UNIT);
    }
}
