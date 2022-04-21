package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import uk.gov.di.ipv.cri.address.library.helpers.KMSSigner;
import uk.gov.di.ipv.cri.address.library.helpers.SignClaimSetJwt;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.ADDRESS_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.DI_CONTEXT;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_ADDRESS_KEY;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_CONTEXT;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_CREDENTIAL_SUBJECT;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VC_TYPE;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.VERIFIABLE_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.address.library.domain.verifiablecredential.VerifiableCredentialConstants.W3_BASE_CONTEXT;

public class VerifiableCredentialService {

    private final SignClaimSetJwt signedClaimSetJwt;
    private final ConfigurationService configurationService;

    public VerifiableCredentialService() {
        this.configurationService = new ConfigurationService();
        this.signedClaimSetJwt =
                new SignClaimSetJwt(
                        new KMSSigner(
                                configurationService.getVerifiableCredentialKmsSigningKeyId()));
    }

    public VerifiableCredentialService(
            SignClaimSetJwt signedClaimSetJwt, ConfigurationService configurationService) {
        this.signedClaimSetJwt = signedClaimSetJwt;
        this.configurationService = configurationService;
    }

    public SignedJWT generateSignedVerifiableCredentialJwt(
            String subject, List<CanonicalAddress> canonicalAddresses) throws JOSEException {
        var now = Instant.now();

        var claimsSet =
                new JWTClaimsSet.Builder()
                        .claim(SUBJECT, subject)
                        .claim(ISSUER, configurationService.getVerifiableCredentialIssuer())
                        .claim(NOT_BEFORE, now.getEpochSecond())
                        .claim(
                                EXPIRATION_TIME,
                                now.plusSeconds(configurationService.getMaxJwtTtl())
                                        .getEpochSecond())
                        .claim(
                                VC_CLAIM,
                                Map.of(
                                        VC_TYPE,
                                        new String[] {
                                            VERIFIABLE_CREDENTIAL_TYPE, ADDRESS_CREDENTIAL_TYPE
                                        },
                                        VC_CONTEXT,
                                        new String[] {W3_BASE_CONTEXT, DI_CONTEXT},
                                        VC_CREDENTIAL_SUBJECT,
                                        Map.of(
                                                VC_ADDRESS_KEY,
                                                canonicalAddresses.stream()
                                                        .map(this::mapToCanonicalReferences)
                                                        .collect(Collectors.toList()))))
                        .build();

        return signedClaimSetJwt.createSignedJwt(claimsSet);
    }

    private Map mapToCanonicalReferences(CanonicalAddress address) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        // TODO:
        // https://github.com/alphagov/digital-identity-architecture/blob/main/rfc/0020-address-structure.md#5-mapping-of-fields-to-data-sources-and-reference-standards
        // Shouldn't have to do this mapping should our canonical model be like the link above
        HashMap mapper = new HashMap();
        address.getValidFrom()
                .ifPresent(anAddress -> mapper.put("validFrom", formatter.format(anAddress)));
        address.getValidUntil()
                .ifPresent(anAddress -> mapper.put("validUntil", formatter.format(anAddress)));
        address.getUprn().ifPresent(uprn -> mapper.put("uprn", uprn));
        mapper.put("organisationName", address.getOrganisationName());
        mapper.put("departmentName", address.getDepartmentName());
        mapper.put("subBuildingName", address.getSubBuildingName());
        mapper.put("buildingNumber", address.getBuildingNumber());
        mapper.put("buildingName", address.getSubBuildingName());
        mapper.put("dependentStreetName", address.getDependentStreetName());
        mapper.put("streetName", address.getStreetName());
        mapper.put("doubleDependentAddressLocality", address.getDoubleDependentLocality());
        mapper.put("dependentAddressLocality", address.getDependentLocality());
        mapper.put("addressLocality", address.getPostTown());
        mapper.put("postalCode", address.getPostcode());
        mapper.put("addressCountry", address.getCountryCode());
        return mapper;
    }
}
