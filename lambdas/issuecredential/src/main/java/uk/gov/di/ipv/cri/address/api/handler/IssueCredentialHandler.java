package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import org.apache.http.HttpStatus;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.service.CredentialIssuerService;

import static org.apache.logging.log4j.Level.ERROR;

public class IssueCredentialHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final String ADDRESS_CREDENTIAL_ISSUER = "address_credential_issuer";
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

        try {
            var sessionId = addressCredentialIssuerService.getSessionId(input);
            var addresses = addressCredentialIssuerService.getAddresses(sessionId);
            eventProbe.counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_OK, 1);
        } catch (CredentialRequestException e) {
            eventProbe.log(ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_BAD_REQUEST, 0);
        } catch (AwsServiceException ex) {
            eventProbe.log(ERROR, ex).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    ex.statusCode(), ex.awsErrorDetails().errorMessage());
        } catch (ParseException e) {
            eventProbe.log(ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.INVALID_REQUEST.getHTTPStatusCode(), OAuth2Error.INVALID_REQUEST);
        }
    }
}
