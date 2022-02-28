package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.SessionHandler.SESSION_ID;

@ExtendWith(MockitoExtension.class)
class SessionHandlerTest {

    @Mock private AddressSessionService addressSessionService;

    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;

    private SessionHandler sessionHandler;

    @BeforeEach
    void setUp() {
        sessionHandler = new SessionHandler(addressSessionService);
    }

    @Test
    void shouldCreateAndSaveAddressSession() throws JsonProcessingException {
        String sessionId = UUID.randomUUID().toString();
        when(addressSessionService.createAndSaveAddressSession()).thenReturn(sessionId);
        APIGatewayProxyResponseEvent responseEvent =
                sessionHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(HttpStatus.SC_CREATED, responseEvent.getStatusCode());
        Map responseBody = new ObjectMapper().readValue(responseEvent.getBody(), Map.class);
        assertEquals(sessionId, responseBody.get(SESSION_ID));
        verifyNoInteractions(apiGatewayProxyRequestEvent);
    }
}
