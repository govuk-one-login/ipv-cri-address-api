package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.service.AddressService;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

import java.util.List;
import java.util.UUID;

public class AddressHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected static final String SESSION_ID = "session_id";
    protected static final String LAMBDA_NAME = "address";

    private final AddressService addressService;
    private final SessionService sessionService;
    private final EventProbe eventProbe;
    private final AuditService auditService;

    @ExcludeFromGeneratedCoverageReport
    public AddressHandler() {
        this(
                new SessionService(),
                new AddressService(),
                new EventProbe(),
                new AuditService(
                        SqsClient.builder().build(),
                        new ConfigurationService(),
                        new ObjectMapper()));
    }

    public AddressHandler(
            SessionService sessionService,
            AddressService addressService,
            EventProbe eventProbe,
            AuditService auditService) {
        this.sessionService = sessionService;
        this.addressService = addressService;
        this.eventProbe = eventProbe;
        this.auditService = auditService;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        String sessionId = input.getHeaders().get(SESSION_ID);
        try {
            List<CanonicalAddress> addresses = addressService.parseAddresses(input.getBody());

            // If we have at least one address, we can return a 201 with the authorization code
            if (!addresses.isEmpty()) {
                SessionItem session = sessionService.validateSessionId(sessionId);

                // Save our addresses to the address table
                addressService.saveAddresses(UUID.fromString(sessionId), addresses);

                auditService.sendAuditEvent(AuditEventTypes.IPV_ADDRESS_CRI_REQUEST_SENT);

                // Now we've saved our address, we need to create an authorization code for the
                // session
                sessionService.createAuthorizationCode(session);

                eventProbe.counterMetric(LAMBDA_NAME);
                return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatusCode.NO_CONTENT, "");
            }

            // If we don't have at least one address, do not save
            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatusCode.OK, "");

        } catch (SessionNotFoundException | SessionExpiredException e) {
            eventProbe.log(Level.ERROR, e).counterMetric(LAMBDA_NAME, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.BAD_REQUEST, e.getMessage());
        } catch (AddressProcessingException | SqsException e) {
            eventProbe.log(Level.ERROR, e).counterMetric(LAMBDA_NAME, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
