package uk.gov.di.ipv.cri.address.api.handler.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.api.handler.IssueCredentialHandler;
import uk.gov.di.ipv.cri.address.api.handler.pact.states.DummyStates;
import uk.gov.di.ipv.cri.address.api.handler.pact.states.InternationalAddressStates;
import uk.gov.di.ipv.cri.address.api.handler.pact.util.Injector;
import uk.gov.di.ipv.cri.address.api.handler.pact.util.MockHttpServer;
import uk.gov.di.ipv.cri.address.api.service.VerifiableCredentialService;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;
import uk.gov.di.ipv.cri.address.library.service.AddressService;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.pact.util.JwtSigner.getEcdsaSigner;
import static uk.gov.di.ipv.cri.address.api.objectmapper.CustomObjectMapper.getMapperWithCustomSerializers;
import static uk.gov.di.ipv.cri.address.api.service.fixtures.TestFixtures.EC_PRIVATE_KEY_1;
import static uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder.ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID;

@Tag("Pact")
@Provider("AddressCriVcProvider")
@PactBroker(
        url = "https://${PACT_BROKER_HOST}",
        authentication =
                @PactBrokerAuth(
                        username = "${PACT_BROKER_USERNAME}",
                        password = "${PACT_BROKER_PASSWORD}"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class InternationalAddressVcTest implements DummyStates, InternationalAddressStates {
    @SystemStub private EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private static final int PORT = 5010;
    private static final boolean ENABLE_FULL_DEBUG = false;
    public static final String SUBJECT = "test-subject";
    private final UUID sessionId = UUID.randomUUID();
    @Mock private SessionService mockSessionService;
    @Mock private AddressService mockAddressService;
    @Mock private EventProbe mockEventProbe;
    @Mock private AuditService mockAuditService;
    @Mock private ConfigurationService mockConfigurationService;
    @InjectMocks private IssueCredentialHandler handler;
    private VerifiableCredentialService verifiableCredentialService;

    @au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
                .tag("AddressCriVcProvider")
                .branch("main", "IpvCoreBack")
                .deployedOrReleased();
    }

    @BeforeAll
    void setup() {
        System.setProperty(
                "pact.filter.description", "Valid credential request for international address");
        System.setProperty("pact.verifier.publishResults", "true");
        System.setProperty("pact.content_type.override.application/jwt", "text");

        if (ENABLE_FULL_DEBUG) {
            // AutoConfig SL4j with Log4J
            BasicConfigurator.configure();
            Configurator.setAllLevels("", Level.DEBUG);
        }
    }

    @AfterEach
    public void tearDown() {
        MockHttpServer.stopServer();
    }

    @BeforeEach
    void pactSetup(PactVerificationContext context)
            throws IOException, JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        environmentVariables.set("LAMBDA_TASK_ROOT", "handler");
        environmentVariables.set(ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID, "override");

        SignedJWTFactory signedJwtFactory = new SignedJWTFactory(getEcdsaSigner());
        ObjectMapper objectMapper = getMapperWithCustomSerializers();

        Clock clock = Clock.fixed(Instant.parse("2099-01-01T00:00:00.00Z"), ZoneId.of("UTC"));
        VerifiableCredentialClaimsSetBuilder claimsSetBuilder =
                new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock);
        claimsSetBuilder.overrideJti("dummyJti");

        when(mockConfigurationService.getVerifiableCredentialIssuer())
                .thenReturn("dummyAddressComponentId");
        verifiableCredentialService =
                new VerifiableCredentialService(
                        signedJwtFactory, mockConfigurationService, objectMapper, claimsSetBuilder);
        handler =
                new IssueCredentialHandler(
                        verifiableCredentialService,
                        mockAddressService,
                        mockSessionService,
                        mockEventProbe,
                        mockAuditService);

        MockHttpServer.startServer(
                new ArrayList<>(List.of(new Injector(handler, "/credential/issue", "/"))), PORT);

        context.setTarget(new HttpTestTarget("localhost", PORT));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTest(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("dummyAccessToken is a valid access token")
    void validDummyAccessToken() throws ParseException {
        AccessToken accessToken =
                AccessToken.parse("Bearer dummyAccessToken", AccessTokenType.BEARER);
        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(getSessionItem());
        AddressItem addressItem = new AddressItem();
        addressItem.setAddresses(getCanonicalAddresses());
        when(mockConfigurationService.getCommonParameterValue(
                        "verifiableCredentialKmsSigningKeyId"))
                .thenReturn(EC_PRIVATE_KEY_1);
        when(mockAddressService.getAddressItem(sessionId)).thenReturn(addressItem);
        when(mockConfigurationService.getMaxJwtTtl()).thenReturn(10L);
        when(mockConfigurationService.getParameterValue("JwtTtlUnit")).thenReturn("MINUTES");
    }

    @NotNull
    private SessionItem getSessionItem() {
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSubject(SUBJECT);
        sessionItem.setSessionId(sessionId);
        sessionItem.setAccessToken("Bearer dummyAccessToken");
        return sessionItem;
    }

    @NotNull
    private List<CanonicalAddress> getCanonicalAddresses() {
        CanonicalAddress internationalAddress = new CanonicalAddress();
        internationalAddress.setAddressCountry("CD");
        internationalAddress.setAddressRegion("North Kivu");
        internationalAddress.setBuildingName("Immeuble Commercial Plaza");
        internationalAddress.setBuildingNumber("4");
        internationalAddress.setSubBuildingName("3");
        internationalAddress.setStreetName("Boulevard Kanyamuhanga");
        internationalAddress.setPostalCode("243");
        internationalAddress.setAddressLocality("Goma");
        internationalAddress.setValidFrom(LocalDate.of(2020, 1, 1));

        return Collections.singletonList(internationalAddress);
    }
}
