package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.common.contenttype.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class IssueCredentialHandlerTest {
    @Mock private Context context;

    @Test
    void shouldReturnAddressWhenIssueCredentialRequestIsValid() {
        String issueCredentialRequestBody = "accesstoken=12345&sub=subject";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();

        event.withBody(issueCredentialRequestBody);
        IssueCredentialHandler handler = new IssueCredentialHandler();
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    }

    @Test
    void shouldReturn400BadRequestWhenIssueCredentialRequestIsInValid() {
        String issueCredentialRequestBody = "accesstoken=12345";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();

        event.withBody(issueCredentialRequestBody);
        IssueCredentialHandler handler = new IssueCredentialHandler();
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
    }
}
