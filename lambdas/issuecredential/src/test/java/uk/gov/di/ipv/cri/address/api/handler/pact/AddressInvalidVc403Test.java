package uk.gov.di.ipv.cri.address.api.handler.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import org.apache.log4j.BasicConfigurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.api.handler.IssueCredentialHandler;
import uk.gov.di.ipv.cri.address.api.handler.pact.states.DummyStates;
import uk.gov.di.ipv.cri.address.api.handler.pact.util.Injector;
import uk.gov.di.ipv.cri.address.api.handler.pact.util.MockHttpServer;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.IssueCredentialHandler.ADDRESS_CREDENTIAL_ISSUER;

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
class AddressInvalidVc403Test implements DummyStates {
    @SystemStub private EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private static final int PORT = 5010;
    private static final boolean ENABLE_FULL_DEBUG = false;
    @Mock private SessionService mockSessionService;
    @Mock private EventProbe mockEventProbe;
    @InjectMocks private IssueCredentialHandler handler;

    @au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
                .tag("AddressCriVcProvider")
                .branch("main", "IpvCoreBack")
                .deployedOrReleased();
    }

    @BeforeAll
    void setup() {
        System.setProperty("pact.filter.description", "Invalid credential request");
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
    void pactSetup(PactVerificationContext context) throws IOException, ParseException {

        environmentVariables.set("LAMBDA_TASK_ROOT", "handler");

        AccessToken accessToken =
                AccessToken.parse("Bearer dummyInvalidAccessToken", AccessTokenType.BEARER);
        when(mockSessionService.getSessionByAccessToken(accessToken))
                .thenThrow(new SessionExpiredException("session expired"));

        MockHttpServer.startServer(
                new ArrayList<>(List.of(new Injector(handler, "/credential/issue", "/"))), PORT);

        context.setTarget(new HttpTestTarget("localhost", PORT));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTest(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("dummyInvalidAccessToken is an invalid access token")
    void validDummyAccessToken() {
        when(mockEventProbe.log(eq(Level.ERROR), Mockito.any(Exception.class)))
                .thenReturn(mockEventProbe);
        when(mockEventProbe.counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d))
                .thenReturn(mockEventProbe);
    }
}
