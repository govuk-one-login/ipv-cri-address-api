package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import uk.gov.di.ipv.cri.address.library.helpers.KMSSigner;
import uk.gov.di.ipv.cri.address.library.helpers.SignClaimSetJwt;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
            String subject, List<CanonicalAddressWithResidency> canonicalAddressWithResidency)
            throws JOSEException {
        var now = Instant.now();
        var claimsSet =
                new JWTClaimsSet.Builder()
                        .claim(SUBJECT, subject)
                        .claim(ISSUER, configurationService.getVerifiableCredentialIssuer())
                        .claim(NOT_BEFORE, now.getEpochSecond())
                        .claim(
                                EXPIRATION_TIME,
                                now.plusSeconds(configurationService.maxJwtTtl()).getEpochSecond())
                        .claim(
                                VC_TYPE,
                                new String[] {VERIFIABLE_CREDENTIAL_TYPE, ADDRESS_CREDENTIAL_TYPE})
                        .claim(
                                VC_CLAIM,
                                Map.of(
                                        VC_TYPE,
                                                new String[] {
                                                    VERIFIABLE_CREDENTIAL_TYPE,
                                                    ADDRESS_CREDENTIAL_TYPE
                                                },
                                        VC_CONTEXT, new String[] {W3_BASE_CONTEXT, DI_CONTEXT},
                                        VC_CREDENTIAL_SUBJECT,
                                                Map.of(VC_ADDRESS_KEY, canonicalAddressWithResidency)))
                        .build();

        return signedClaimSetJwt.createSignedJwt(claimsSet);
    }
}
