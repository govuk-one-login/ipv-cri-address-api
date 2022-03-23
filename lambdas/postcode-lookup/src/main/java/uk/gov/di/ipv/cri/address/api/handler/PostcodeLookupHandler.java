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
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupValidationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;
import uk.gov.di.ipv.cri.address.library.service.PostcodeLookupService;

import java.util.List;
import java.util.Map;

public class PostcodeLookupHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final PostcodeLookupService postcodeLookupService;
    private final AddressSessionService addressSessionService;
    private EventProbe eventProbe;
    protected static final String SESSION_ID = "session_id";

    public PostcodeLookupHandler(
            PostcodeLookupService postcodeLookupService,
            AddressSessionService addressSessionService,
            EventProbe eventProbe) {
        this.postcodeLookupService = postcodeLookupService;
        this.addressSessionService = addressSessionService;
        this.eventProbe = eventProbe;
    }

    @ExcludeFromGeneratedCoverageReport
    public PostcodeLookupHandler() {

        this.postcodeLookupService = new PostcodeLookupService();
        this.addressSessionService = new AddressSessionService();
        this.eventProbe = new EventProbe();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        String sessionId = input.getHeaders().get(SESSION_ID);
        eventProbe.addDimensions(Map.of(SESSION_ID, sessionId));

        String postcode = input.getPathParameters().get("postcode");
        eventProbe.addDimensions(Map.of("postcode", postcode));

        try {
            addressSessionService.validateSessionId(sessionId);
            List<CanonicalAddress> results = postcodeLookupService.lookupPostcode(postcode);
            eventProbe.addDimensions(Map.of("result_count", String.valueOf(results.size())));

            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_OK, results);

        } catch (PostcodeLookupValidationException e) {
            eventProbe.log(Level.ERROR, e);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_BAD_REQUEST, ErrorResponse.INVALID_POSTCODE);
        } catch (SessionValidationException e) {
            eventProbe.log(Level.ERROR, e);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_BAD_REQUEST, e.getMessage());
        } catch (SessionNotFoundException e) {
            eventProbe.log(Level.ERROR, e);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_NOT_FOUND, e.getMessage());
        } catch (SessionExpiredException e) {
            eventProbe.log(Level.ERROR, e);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_GONE, e.getMessage());
        } catch (Exception e) {
            eventProbe.log(Level.ERROR, e);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, ErrorResponse.SERVER_ERROR);
        }
    }
}
