package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;

import java.util.Map;

public class SessionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    protected static final String SESSION_ID = "session_id";
    private AddressSessionService addressSessionService;

    public SessionHandler() {
        addressSessionService = new AddressSessionService();
    }

    @ExcludeFromGeneratedCoverageReport
    public SessionHandler(AddressSessionService addressSessionService) {
        this.addressSessionService = addressSessionService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        // todo validate request params and claims JWT

        String sessionId = addressSessionService.createAndSaveAddressSession();

        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent =
                new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setStatusCode(HttpStatus.SC_CREATED);
        Map<String, String> responseMap = Map.of(SESSION_ID, sessionId);
        apiGatewayProxyResponseEvent.setBody(new Gson().toJson(responseMap));
        return apiGatewayProxyResponseEvent;
    }
}
