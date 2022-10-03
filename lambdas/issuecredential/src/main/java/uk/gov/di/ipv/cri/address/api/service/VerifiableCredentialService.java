package uk.gov.di.ipv.cri.address.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.util.KMSSigner;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.ADDRESS_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.DI_CONTEXT;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.VC_ADDRESS_KEY;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.VC_CLAIM;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.VC_CONTEXT;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.VC_CREDENTIAL_SUBJECT;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.VC_TYPE;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.VERIFIABLE_CREDENTIAL_TYPE;
import static uk.gov.di.ipv.cri.address.api.domain.VerifiableCredentialConstants.W3_BASE_CONTEXT;

public class VerifiableCredentialService {

    private final SignedJWTFactory signedJwtFactory;
    private final ConfigurationService configurationService;

    private final ObjectMapper objectMapper;

    public VerifiableCredentialService() {
        this.configurationService = new ConfigurationService();
        this.signedJwtFactory =
                new SignedJWTFactory(
                        new KMSSigner(
                                configurationService.getCommonParameterValue(
                                        "verifiableCredentialKmsSigningKeyId")));
        this.objectMapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule());
    }

    public VerifiableCredentialService(
            SignedJWTFactory signedClaimSetJwt,
            ConfigurationService configurationService,
            ObjectMapper objectMapper) {
        this.signedJwtFactory = signedClaimSetJwt;
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
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
                                                convertAddresses(canonicalAddresses))))
                        .build();

        return signedJwtFactory.createSignedJwt(claimsSet);
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
