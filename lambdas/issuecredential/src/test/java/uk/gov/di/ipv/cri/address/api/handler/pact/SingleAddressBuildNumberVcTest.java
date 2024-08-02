package uk.gov.di.ipv.cri.address.api.handler.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
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
import uk.gov.di.ipv.cri.address.api.handler.pact.states.SingleAddressBuildingNumberStates;
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
import static uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder.ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID;

@Tag("Pact")
@Provider("AddressCriVcProvider")
@PactFolder("pacts")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class SingleAddressBuildNumberVcTest implements DummyStates, SingleAddressBuildingNumberStates {
    @SystemStub private EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private static final int PORT = 5010;
    private static final boolean ENABLE_FULL_DEBUG = true;
    public static final String SUBJECT = "test-subject";
    private final UUID sessionId = UUID.randomUUID();
    @Mock private SessionService mockSessionService;
    @Mock private AddressService mockAddressService;
    @Mock private EventProbe mockEventProbe;
    @Mock private AuditService mockAuditService;
    @Mock private ConfigurationService mockConfigurationService;
    @InjectMocks private IssueCredentialHandler handler;
    private VerifiableCredentialService verifiableCredentialService;

    @BeforeAll
    void setup() {
        System.setProperty("pact.filter.description", "Valid credential request for Experian VC");
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
        when(mockAddressService.getAddressItem(sessionId)).thenReturn(addressItem);
        when(mockConfigurationService.getVerifiableCredentialIssuer())
                .thenReturn("dummyAddressComponentId");
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
        CanonicalAddress address = new CanonicalAddress();
        address.setAddressCountry("GB");
        address.setBuildingName("");
        address.setStreetName("HADLEY ROAD");
        address.setPostalCode("BA2 5AA");
        address.setBuildingNumber("8");
        address.setAddressLocality("BATH");
        address.setPostalCode("BA2 5AA");
        address.setValidFrom(LocalDate.of(2000, 1, 1));

        return Collections.singletonList(address);
    }
}