package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exceptions.ServerException;
import uk.gov.di.ipv.cri.address.library.exceptions.ValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.helpers.DomainProbe;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;

import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;
import static software.amazon.awssdk.http.HttpStatusCode.BAD_REQUEST;
import static software.amazon.awssdk.http.HttpStatusCode.CREATED;
import static software.amazon.awssdk.http.HttpStatusCode.INTERNAL_SERVER_ERROR;

public class SessionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected static final String SESSION_ID = "session_id";
    private final AddressSessionService addressSessionService;
    private final DomainProbe domainProbe;

    public SessionHandler() {
        addressSessionService = new AddressSessionService();
        domainProbe = new DomainProbe();
    }

    public SessionHandler(AddressSessionService addressSessionService, DomainProbe domainProbe) {
        this.addressSessionService = addressSessionService;
        this.domainProbe = domainProbe;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        try {

            SessionRequest sessionRequest =
                    addressSessionService.validateSessionRequest(input.getBody());

            domainProbe.addDimensions(Map.of("issuer", sessionRequest.getClientId()));

            UUID sessionId = addressSessionService.createAndSaveAddressSession(sessionRequest);

            domainProbe.counterMetric("session_created").auditEvent(sessionRequest);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    CREATED, Map.of(SESSION_ID, sessionId.toString()));

        } catch (ValidationException e) {

            domainProbe.log(INFO, e).counterMetric("session_created", 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    BAD_REQUEST, ErrorResponse.SESSION_VALIDATION_ERROR);
        } catch (ServerException e) {

            domainProbe.log(ERROR, e).counterMetric("session_created", 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    INTERNAL_SERVER_ERROR, ErrorResponse.SERVER_CONFIG_ERROR);
        }
    }
}
