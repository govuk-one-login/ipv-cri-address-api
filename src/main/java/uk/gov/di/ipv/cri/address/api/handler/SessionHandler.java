package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

public class SessionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = input.getHeaders();
        System.out.println("👍 headers is " + new Gson().toJson(headers));
        System.out.println("👍 input is " + input.getBody());
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent =
                new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setStatusCode(HttpStatus.SC_CREATED);
        UUID uuid = UUID.randomUUID();
        apiGatewayProxyResponseEvent.setBody(uuid.toString());
        return apiGatewayProxyResponseEvent;
    }
}
