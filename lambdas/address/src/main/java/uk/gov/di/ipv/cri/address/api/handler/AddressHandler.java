package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;

import java.util.*;

public class AddressHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected static final String SESSION_ID = "session_id";

    Logger log = LogManager.getLogger();

    private AddressSessionService sessionService;

    public AddressHandler() {
        sessionService = new AddressSessionService();
    }

    public AddressHandler(AddressSessionService sessionService) {
        this.sessionService = sessionService;
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
            sessionService.saveAddresses(sessionId, addresses);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_CREATED, Map.of(SESSION_ID, sessionId));

        } catch (SessionValidationException
                | SessionNotFoundException
                | SessionExpiredException e) {
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_BAD_REQUEST, e.getMessage());
        } catch (AddressProcessingException e) {
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
