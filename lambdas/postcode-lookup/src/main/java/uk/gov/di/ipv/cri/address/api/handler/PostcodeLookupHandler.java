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
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.*;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;
import uk.gov.di.ipv.cri.address.library.service.PostcodeLookupService;

import java.util.List;

public class PostcodeLookupHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final PostcodeLookupService postcodeLookupService;
    private final AddressSessionService addressSessionService;
    final Logger log = LogManager.getLogger();
    protected static final String SESSION_ID = "session_id";

    public PostcodeLookupHandler(
            PostcodeLookupService postcodeLookupService,
            AddressSessionService addressSessionService) {
        this.postcodeLookupService = postcodeLookupService;
        this.addressSessionService = addressSessionService;
    }

    @ExcludeFromGeneratedCoverageReport
    public PostcodeLookupHandler() {

        this.postcodeLookupService = new PostcodeLookupService();
        this.addressSessionService = new AddressSessionService();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        String sessionId = input.getHeaders().get(SESSION_ID);

        String postcode = input.getPathParameters().get("postcode");
        log.debug("Postcode: {}", postcode);

        try {
            addressSessionService.validateSessionId(sessionId);

            log.info("Session passed validation");
            List<CanonicalAddress> results = postcodeLookupService.lookupPostcode(postcode);

            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_OK, results);

        } catch (PostcodeLookupProcessingException e) {
            log.error("PostcodeLookupProcessingException: {}", e.getMessage());
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, ErrorResponse.SERVER_ERROR);
        } catch (PostcodeLookupValidationException e) {
            log.error("PostcodeLookupValidationException: {}", e.getMessage());
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_BAD_REQUEST, ErrorResponse.INVALID_POSTCODE);
        } catch (SessionValidationException e) {
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_BAD_REQUEST, e.getMessage());
        } catch (SessionNotFoundException e) {
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_NOT_FOUND, e.getMessage());
        } catch (SessionExpiredException e) {
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_GONE, e.getMessage());
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, ErrorResponse.SERVER_ERROR);
        }
    }
}
