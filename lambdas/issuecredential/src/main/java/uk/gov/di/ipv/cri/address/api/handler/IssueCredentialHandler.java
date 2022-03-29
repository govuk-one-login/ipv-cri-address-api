package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import org.apache.http.HttpStatus;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;

import java.net.URI;
import java.util.List;
import java.util.Set;

public class IssueCredentialHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String ACCESS_TOKEN = "accesstoken";
    private static final String SUB = "sub";

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        URI arbitraryUri = URI.create("https://gds");
        HTTPRequest request = new HTTPRequest(HTTPRequest.Method.POST, arbitraryUri);
        request.setQuery(input.getBody());

        boolean isValidIssueRequest =
                request.getQueryParameters().keySet().containsAll(Set.of(ACCESS_TOKEN, SUB));

        int status = isValidIssueRequest ? HttpStatus.SC_OK : HttpStatus.SC_BAD_REQUEST;

        return ApiGatewayResponseGenerator.proxyJsonResponse(
                status, List.of(new CanonicalAddressWithResidency()));
    }
}
