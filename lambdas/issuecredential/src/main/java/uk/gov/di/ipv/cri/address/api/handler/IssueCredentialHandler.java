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
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.service.CredentialIssuerService;

public class IssueCredentialHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private EventProbe eventProbe;
    private final CredentialIssuerService addressCredentialIssuerService;

    public IssueCredentialHandler(
            CredentialIssuerService addressCredentialIssuerService, EventProbe eventProbe) {
        this.addressCredentialIssuerService = addressCredentialIssuerService;
        this.eventProbe = eventProbe;
    }

    public IssueCredentialHandler() {
        this.addressCredentialIssuerService = new CredentialIssuerService();
        this.eventProbe = new EventProbe();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        APIGatewayProxyResponseEvent response;
        try {
            var sessionId = addressCredentialIssuerService.getSessionId(input);
            var addresses = addressCredentialIssuerService.getAddresses(sessionId);
            eventProbe.counterMetric("credential");
            response = ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_OK, 1);
        } catch (CredentialRequestException e) {
            eventProbe.log(Level.ERROR, e).counterMetric("credential", 0d);
            response = ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_BAD_REQUEST, 0);
        }
        return response;
    }
}
