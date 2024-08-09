package uk.gov.di.ipv.cri.address.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;

import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.ADDRESS_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.DI_CONTEXT;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.VC_ADDRESS_KEY;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.W3_BASE_CONTEXT;

public class VerifiableCredentialService {
    private final VerifiableCredentialClaimsSetBuilder vcClaimsSetBuilder;
    private final SignedJWTFactory signedJwtFactory;
    private final ConfigurationService configurationService;
    private final ObjectMapper objectMapper;

    public VerifiableCredentialService(
            SignedJWTFactory signedClaimSetJwt,
            ConfigurationService configurationService,
            ObjectMapper objectMapper,
            VerifiableCredentialClaimsSetBuilder vcClaimsSetBuilder) {
        this.signedJwtFactory = signedClaimSetJwt;
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
        this.vcClaimsSetBuilder = vcClaimsSetBuilder;
    }

    public SignedJWT generateSignedVerifiableCredentialJwt(
            String subject, List<CanonicalAddress> canonicalAddresses)
            throws JOSEException, JsonProcessingException, ParseException {
        long jwtTtl = this.configurationService.getMaxJwtTtl();
        ChronoUnit jwtTtlUnit =
                ChronoUnit.valueOf(this.configurationService.getParameterValue("JwtTtlUnit"));
        var claimsSet =
                this.vcClaimsSetBuilder
                        .subject(subject)
                        .timeToLive(jwtTtl, jwtTtlUnit)
                        .verifiableCredentialType(ADDRESS_CREDENTIAL_TYPE)
                        .verifiableCredentialContext(new String[] {W3_BASE_CONTEXT, DI_CONTEXT})
                        .verifiableCredentialSubject(
                                Map.of(VC_ADDRESS_KEY, convertAddresses(canonicalAddresses)))
                        .build();

        return signedJwtFactory.createSignedJwt(objectMapper.writeValueAsString(claimsSet));
    }

    public Map<String, Object> getAuditEventExtensions(List<CanonicalAddress> addresses) {
        return Map.of(
                ISSUER,
                Objects.requireNonNull(
                        configurationService.getVerifiableCredentialIssuer(),
                        "VC issuer must not be null"),
                "addressesEntered",
                Objects.nonNull(addresses) ? addresses.size() : 0);
    }

    private Object[] convertAddresses(List<CanonicalAddress> addresses) {
        return addresses.stream()
                .map(address -> objectMapper.convertValue(address, Map.class))
                .toArray();
    }
}
