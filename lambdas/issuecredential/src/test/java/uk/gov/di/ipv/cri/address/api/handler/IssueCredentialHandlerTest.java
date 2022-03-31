package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.common.contenttype.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.service.CredentialIssuerService;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueCredentialHandlerTest {
    @Mock private Context context;
    @Mock private CredentialIssuerService mockAddressCredentialIssuerService;
    private IssueCredentialHandler handler;

    @BeforeEach
    void setUp() {
        handler = new IssueCredentialHandler(mockAddressCredentialIssuerService);
    }

    @Test
    void shouldReturnAddressAndAOneValueWhenIssueCredentialRequestIsValid()
            throws CredentialRequestException {
        UUID sessionId = UUID.randomUUID();
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withBody("sub=12345");
        event.withHeaders(Map.of(CredentialIssuerService.AUTHORIZATION, "access-token"));
        when(mockAddressCredentialIssuerService.getSessionId(event)).thenReturn(sessionId);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockAddressCredentialIssuerService).getAddresses(sessionId);
        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(response.getBody(), "1");
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenSubjectIsNotSupplied()
            throws CredentialRequestException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withHeaders(
                Map.of(CredentialIssuerService.AUTHORIZATION, UUID.randomUUID().toString()));

        when(mockAddressCredentialIssuerService.getSessionId(event))
                .thenThrow(CredentialRequestException.class);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals(response.getBody(), "0");
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenAuthorizationHeaderIsNotSupplied()
            throws CredentialRequestException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withBody("sub=subject");

        when(mockAddressCredentialIssuerService.getSessionId(event))
                .thenThrow(CredentialRequestException.class);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals(response.getBody(), "0");
    }
}
