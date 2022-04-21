package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.helpers.SignClaimSetJwt;
import uk.gov.di.ipv.cri.address.library.helpers.fixtures.TestFixtures;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_ADDRESS_KEY;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_CREDENTIAL_SUBJECT;

@ExtendWith(MockitoExtension.class)
public class VerifiableCredentialServiceTest implements TestFixtures {
    public static final String SUBJECT = "subject";
    public static final String UPRN = "72262801";
    public static final String BUILDING_NUMBER = "8";
    public static final String STREET_NAME = "GRANGE FIELDS WAY";
    public static final String POST_TOWN = "LEEDS";
    public static final String POSTCODE = "LS10 4QL";
    public static final String COUNTRY_CODE = "GBR";
    public static final Instant VALID_FROM = Instant.parse("2010-02-26T00:00:00.00Z");
    public static final Instant VALID_UNTIL = Instant.parse("2021-01-16T00:00:00.00Z");
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock private ConfigurationService mockConfigurationService;

    @BeforeEach
    void setUp() {
        when(mockConfigurationService.getVerifiableCredentialIssuer())
                .thenReturn("https://address-cri.account.gov.uk.TBC");
    }

    @Test
    void shouldReturnAVerifiedCredentialWhenGivenCanonicalAddresses() throws JOSEException {
        SignClaimSetJwt mockSignedClaimSetJwt = mock(SignClaimSetJwt.class);
        var verifiableCredentialService =
                new VerifiableCredentialService(mockSignedClaimSetJwt, mockConfigurationService);

        when(mockConfigurationService.getVerifiableCredentialIssuer())
                .thenReturn("address-cri-issue");
        when(mockConfigurationService.getMaxJwtTtl()).thenReturn(342L);

        var canonicalAddresses = List.of(mock(CanonicalAddress.class));

        verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                SUBJECT, canonicalAddresses);

        verify(mockSignedClaimSetJwt).createSignedJwt(any());
    }

    @Test
    void shouldCreateValidSignedJWT()
            throws InvalidKeySpecException, NoSuchAlgorithmException, JOSEException, ParseException,
                    JsonProcessingException {

        AddressSessionItem addressSessionItem = new AddressSessionItem();
        addressSessionItem.setSubject(SUBJECT);
        CanonicalAddress address = new CanonicalAddress();
        address.setUprn(Long.valueOf(UPRN));
        address.setBuildingNumber(BUILDING_NUMBER);
        address.setStreetName(STREET_NAME);
        address.setPostTown(POST_TOWN);
        address.setPostcode(POSTCODE);
        address.setCountryCode(COUNTRY_CODE);
        address.setValidFrom(Date.from(VALID_FROM));
        address.setValidUntil(Date.from(VALID_UNTIL));
        List<CanonicalAddress> canonicalAddresses = List.of(address);
        addressSessionItem.setAddresses(canonicalAddresses);

        SignClaimSetJwt signClaimSetJwt = new SignClaimSetJwt(new ECDSASigner(getPrivateKey()));
        var verifiableCredentialService =
                new VerifiableCredentialService(signClaimSetJwt, mockConfigurationService);

        SignedJWT signedJWT =
                verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        SUBJECT, canonicalAddresses);
        JWTClaimsSet generatedClaims = signedJWT.getJWTClaimsSet();
        assertTrue(signedJWT.verify(new ECDSAVerifier(ECKey.parse(EC_PUBLIC_JWK_1))));

        JsonNode claimsSet = objectMapper.readTree(generatedClaims.toString());

        assertEquals(5, claimsSet.size());

        JsonNode claims = claimsSet;
        assertAll(
                () -> {
                    assertEquals(
                            address.getUprn().get().toString(),
                            claims.get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_ADDRESS_KEY)
                                    .get(0)
                                    .get("uprn")
                                    .asText());
                    assertEquals(
                            address.getBuildingNumber(),
                            claims.get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_ADDRESS_KEY)
                                    .get(0)
                                    .get("buildingNumber")
                                    .asText());
                    assertEquals(
                            address.getStreetName(),
                            claims.get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_ADDRESS_KEY)
                                    .get(0)
                                    .get("streetName")
                                    .asText());
                    assertEquals(
                            address.getPostTown(),
                            claims.get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_ADDRESS_KEY)
                                    .get(0)
                                    .get("addressLocality")
                                    .asText());
                    assertEquals(
                            address.getPostcode(),
                            claims.get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_ADDRESS_KEY)
                                    .get(0)
                                    .get("postalCode")
                                    .asText());
                    assertEquals(
                            address.getCountryCode(),
                            claims.get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_ADDRESS_KEY)
                                    .get(0)
                                    .get("addressCountry")
                                    .asText());
                    assertEquals(
                            "2010-02-26",
                            claims.get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_ADDRESS_KEY)
                                    .get(0)
                                    .get("validFrom")
                                    .asText());
                    assertEquals(
                            "2021-01-16",
                            claims.get(VC_CLAIM)
                                    .get(VC_CREDENTIAL_SUBJECT)
                                    .get(VC_ADDRESS_KEY)
                                    .get(0)
                                    .get("validUntil")
                                    .asText());
                });
        ECDSAVerifier ecVerifier = new ECDSAVerifier(ECKey.parse(EC_PUBLIC_JWK_1));
        assertTrue(signedJWT.verify(ecVerifier));
    }
}
