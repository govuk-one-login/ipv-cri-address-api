package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;
import uk.gov.di.ipv.cri.address.library.service.AccessTokenService;

public class AccessTokenHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private EventProbe eventProbe;
    private final AccessTokenService accessTokenService;

    public AccessTokenHandler(AccessTokenService accessTokenService, EventProbe eventProbe) {
        this.accessTokenService = accessTokenService;
        this.eventProbe = eventProbe;
    }

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

            AddressSessionItem addressSessionItem =
                    accessTokenService.getAddressSession(tokenRequest);

            TokenResponse tokenResponse = accessTokenService.createToken(tokenRequest);
            AccessTokenResponse accessTokenResponse = tokenResponse.toSuccessResponse();
            accessTokenService.writeToken(accessTokenResponse, addressSessionItem);

            eventProbe.counterMetric("accesstoken");

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_OK, accessTokenResponse.toJSONObject());

        } catch (ParseException e) {
            eventProbe.log(Level.ERROR, e).counterMetric("accesstoken", 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    getHttpStatusCodeForErrorResponse(e.getErrorObject()),
                    e.getErrorObject().toJSONObject());
        }
    }

    private int getHttpStatusCodeForErrorResponse(ErrorObject errorObject) {
        return errorObject.getHTTPStatusCode() > 0
                ? errorObject.getHTTPStatusCode()
                : HttpStatus.SC_BAD_REQUEST;
    }
}
