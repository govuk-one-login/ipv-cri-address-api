package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;
import uk.gov.di.ipv.cri.address.library.service.AccessTokenService;

public class AccessTokenHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private EventProbe eventProbe;
    private final AccessTokenService accessTokenService;
    static final String METRIC_NAME_ACCESS_TOKEN = "accesstoken";

    public AccessTokenHandler(AccessTokenService accessTokenService, EventProbe eventProbe) {
        this.accessTokenService = accessTokenService;
        this.eventProbe = eventProbe;
    }

    @ExcludeFromGeneratedCoverageReport
    public AccessTokenHandler() {
        this.accessTokenService = new AccessTokenService();
        this.eventProbe = new EventProbe();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        try {
            TokenRequest tokenRequest = accessTokenService.createTokenRequest(input.getBody());
            accessTokenService.validateTokenRequest(tokenRequest);

            AddressSessionItem addressSessionItem =
                    accessTokenService.getAddressSession(tokenRequest);

            AccessTokenResponse accessTokenResponse = accessTokenService.createToken(tokenRequest);
            accessTokenService.writeToken(accessTokenResponse, addressSessionItem);

            eventProbe.counterMetric(METRIC_NAME_ACCESS_TOKEN);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_OK, accessTokenResponse.toJSONObject());

        } catch (AccessTokenValidationException e) {
            eventProbe.log(Level.INFO, e).counterMetric(METRIC_NAME_ACCESS_TOKEN, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_BAD_REQUEST, ErrorResponse.TOKEN_VALIDATION_ERROR);
        }
    }
}
