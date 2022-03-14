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
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.service.PostcodeLookupService;

import java.util.List;

public class PostcodeLookupHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final PostcodeLookupService postcodeLookupService;
    Logger log = LogManager.getLogger();

    public PostcodeLookupHandler(PostcodeLookupService postcodeLookupService) {
        this.postcodeLookupService = postcodeLookupService;
    }

    @ExcludeFromGeneratedCoverageReport
    public PostcodeLookupHandler() {
        this.postcodeLookupService = new PostcodeLookupService();
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        String postcode = input.getPathParameters().get("postcode");
        log.debug("Postcode: {}", postcode);

        try {
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
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, ErrorResponse.SERVER_ERROR);
        }
    }
}
