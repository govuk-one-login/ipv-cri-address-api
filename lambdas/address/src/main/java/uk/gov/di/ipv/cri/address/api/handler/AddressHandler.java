package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.models.AuthorizationResponse;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;

import java.util.*;

public class AddressHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected static final String SESSION_ID = "session_id";
    protected static final String LAMBDA_NAME = "address";

    private final AddressSessionService sessionService;

    private EventProbe eventProbe;

    public AddressHandler() {
        sessionService = new AddressSessionService();
        eventProbe = new EventProbe();
    }

    public AddressHandler(AddressSessionService sessionService, EventProbe eventProbe) {
        this.sessionService = sessionService;
        this.eventProbe = eventProbe;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        String sessionId = input.getHeaders().get(SESSION_ID);
        try {
            List<CanonicalAddressWithResidency> addresses =
                    sessionService.parseAddresses(input.getBody());

            // If we have at least one address, we can return a 201 with the authorization code
            if (!addresses.isEmpty()) {
                AddressSessionItem session = sessionService.saveAddresses(sessionId, addresses);
                eventProbe.counterMetric(LAMBDA_NAME);
                return ApiGatewayResponseGenerator.proxyJsonResponse(
                        HttpStatus.SC_CREATED, new AuthorizationResponse(session));
            }

            // If we don't have at least one address, do not save
            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_OK, "");

        } catch (SessionNotFoundException | SessionExpiredException e) {
            eventProbe.log(Level.ERROR, e).counterMetric(LAMBDA_NAME, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_BAD_REQUEST, e.getMessage());
        } catch (AddressProcessingException e) {
            eventProbe.log(Level.ERROR, e).counterMetric(LAMBDA_NAME, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
