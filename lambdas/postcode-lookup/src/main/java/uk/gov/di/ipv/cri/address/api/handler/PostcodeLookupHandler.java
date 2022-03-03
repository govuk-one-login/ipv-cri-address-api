package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.http.HttpStatusCode;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.models.PostcodeResult;
import uk.gov.di.ipv.cri.address.library.service.PostcodeLookupService;

import java.util.ArrayList;

public class PostcodeLookupHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final PostcodeLookupService postcodeLookupService;

    public PostcodeLookupHandler(PostcodeLookupService postcodeLookupService) {
        this.postcodeLookupService = postcodeLookupService;
    }

    @ExcludeFromGeneratedCoverageReport
    public PostcodeLookupHandler() {
        this.postcodeLookupService = new PostcodeLookupService();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        LambdaLogger logger = context.getLogger();
        logger.log("PostcodeLookup Invoked");
        logger.log("Input: " + input.getBody());

        String postcode = input.getBody();

        try {
            ArrayList<PostcodeResult> results = postcodeLookupService.lookupPostcode(postcode);

            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatusCode.OK, results);

        } catch (PostcodeLookupProcessingException e) {
            logger.log("PostcodeLookupProcessingException: " + e.getMessage());
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, ErrorResponse.SERVER_ERROR);
        } catch (PostcodeLookupValidationException e) {
            logger.log("PostcodeLookupValidationException: " + e.getMessage());
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.BAD_REQUEST, ErrorResponse.INVALID_POSTCODE);
        }
    }
}
