package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import org.apache.logging.log4j.Level;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.service.AddressService;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.deserializers.PiiRedactingDeserializer;

import java.util.List;
import java.util.UUID;

import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_EXPIRED;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_NOT_FOUND;

public class AddressHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected static final String SESSION_ID = "session_id";
    protected static final String LAMBDA_NAME = "address";

    private final AddressService addressService;
    private final SessionService sessionService;
    private final EventProbe eventProbe;
    private final long addressTtl;

    @ExcludeFromGeneratedCoverageReport
    public AddressHandler() {
        ClientProviderFactory clientProviderFactory = new ClientProviderFactory();

        ObjectMapper objectMapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule())
                        .registerModule(
                                new SimpleModule()
                                        .addDeserializer(
                                                CanonicalAddress.class,
                                                new PiiRedactingDeserializer<>(
                                                        CanonicalAddress.class)));

        ConfigurationService configurationService =
                new ConfigurationService(
                        clientProviderFactory.getSSMProvider(),
                        clientProviderFactory.getSecretsProvider());

        addressTtl = configurationService.getSessionExpirationEpoch();

        this.sessionService =
                new SessionService(
                        configurationService, clientProviderFactory.getDynamoDbEnhancedClient());
        this.addressService = new AddressService(configurationService, objectMapper);
        this.eventProbe = new EventProbe();
    }

    public AddressHandler(
            SessionService sessionService,
            AddressService addressService,
            EventProbe eventProbe,
            long addressTtl) {
        this.sessionService = sessionService;
        this.addressService = addressService;
        this.eventProbe = eventProbe;
        this.addressTtl = addressTtl;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST, clearState = true)
    @Metrics(captureColdStart = true)
    @Tracing
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        String sessionId = input.getHeaders().get(SESSION_ID);
        try {
            List<CanonicalAddress> addresses = addressService.parseAddresses(input.getBody());

            // If we have at least one address, we can return a 201 with the authorization code
            if (!addresses.isEmpty()) {
                SessionItem session = sessionService.validateSessionId(sessionId);
                eventProbe.log(Level.INFO, "found session");

                // Links validUntil in a PREVIOUS address to validFrom in a CURRENT
                addressService.setAddressValidity(addresses);

                // Save our addresses to the address table
                addressService.saveAddresses(UUID.fromString(sessionId), addresses, addressTtl);

                // Now we've saved our address, we need to create an authorization code for the
                // session
                sessionService.createAuthorizationCode(session);

                eventProbe.counterMetric(LAMBDA_NAME);
                return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatusCode.NO_CONTENT, "");
            }

            // If we don't have at least one address, do not save
            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatusCode.OK, "");

        } catch (SessionNotFoundException e) {
            eventProbe.log(Level.ERROR, e).counterMetric(LAMBDA_NAME, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.ACCESS_DENIED.getHTTPStatusCode(),
                    OAuth2Error.ACCESS_DENIED
                            .appendDescription(" - " + SESSION_NOT_FOUND.getErrorSummary())
                            .toJSONObject());
        } catch (SessionExpiredException e) {
            eventProbe.log(Level.ERROR, e).counterMetric(LAMBDA_NAME, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.ACCESS_DENIED.getHTTPStatusCode(),
                    OAuth2Error.ACCESS_DENIED
                            .appendDescription(" - " + SESSION_EXPIRED.getErrorSummary())
                            .toJSONObject());
        } catch (AddressProcessingException e) {
            eventProbe.log(Level.ERROR, e).counterMetric(LAMBDA_NAME, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
