package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.service.PostcodeLookupService;

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
        return null;
    }
}
