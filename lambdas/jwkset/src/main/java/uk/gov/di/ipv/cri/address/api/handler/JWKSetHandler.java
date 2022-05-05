package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.apache.http.HttpStatus;
import software.amazon.awssdk.http.Header;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;

import java.util.List;
import java.util.Map;

public class JWKSetHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final JWKSetService jwkSetService;

    public JWKSetHandler(JWKSetService jwkSetService) {
        this.jwkSetService = jwkSetService;
    }

    public JWKSetHandler() {
        this.jwkSetService = new JWKSetService();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        List<JWK> jwks = jwkSetService.getJWKs();
        JWKSet jwkSet = new JWKSet(jwks);
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent =
                new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setStatusCode(HttpStatus.SC_OK);
        apiGatewayProxyResponseEvent.setBody(jwkSet.toString());
        apiGatewayProxyResponseEvent.setHeaders(Map.of(Header.CONTENT_TYPE, JWKSet.MIME_TYPE));
        return apiGatewayProxyResponseEvent;
    }
}
