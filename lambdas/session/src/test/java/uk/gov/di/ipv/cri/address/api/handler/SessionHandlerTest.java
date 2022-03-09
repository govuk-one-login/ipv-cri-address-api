package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exceptions.ServerException;
import uk.gov.di.ipv.cri.address.library.exceptions.ValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.DomainProbe;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.SessionHandler.SESSION_ID;

@ExtendWith(MockitoExtension.class)
class SessionHandlerTest {

    @Mock private AddressSessionService addressSessionService;

    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;

    @Mock private SessionRequest sessionRequest;

    @Mock private DomainProbe domainProbe;

    private SessionHandler sessionHandler;

    @BeforeEach
    void setUp() {
        sessionHandler = new SessionHandler(addressSessionService, domainProbe);
    }

    @Test
    void shouldCreateAndSaveAddressSession()
            throws ValidationException, ServerException, JsonProcessingException {

        when(domainProbe.counterMetric(anyString())).thenReturn(domainProbe);

        UUID sessionId = UUID.randomUUID();
        when(sessionRequest.getClientId()).thenReturn("ipv-core");
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");
        when(addressSessionService.validateSessionRequest("some json")).thenReturn(sessionRequest);
        when(addressSessionService.createAndSaveAddressSession(sessionRequest))
                .thenReturn(sessionId);

        APIGatewayProxyResponseEvent responseEvent =
                sessionHandler.handleRequest(apiGatewayProxyRequestEvent, null);

        assertEquals(HttpStatus.SC_CREATED, responseEvent.getStatusCode());
        Map responseBody = new ObjectMapper().readValue(responseEvent.getBody(), Map.class);
        assertEquals(sessionId.toString(), responseBody.get(SESSION_ID));

        verify(domainProbe).addDimensions(Map.of("issuer", "ipv-core"));
        verify(domainProbe).counterMetric("session_created");
    }

    @Test
    void shouldCatchValidationExceptionAndReturn400Response()
            throws ValidationException, ServerException, JsonProcessingException {

        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");
        ValidationException validationException = new ValidationException("");
        when(addressSessionService.validateSessionRequest("some json"))
                .thenThrow(validationException);
        setupDomainProbeErrorBehaviour();

        APIGatewayProxyResponseEvent responseEvent =
                sessionHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(HttpStatus.SC_BAD_REQUEST, responseEvent.getStatusCode());
        Map responseBody = new ObjectMapper().readValue(responseEvent.getBody(), Map.class);
        assertEquals(ErrorResponse.SESSION_VALIDATION_ERROR.getCode(), responseBody.get("code"));
        assertEquals(
                ErrorResponse.SESSION_VALIDATION_ERROR.getMessage(), responseBody.get("message"));

        verify(domainProbe).counterMetric("session_created", 0d);
        verify(domainProbe).log(Level.INFO, validationException);
        verify(addressSessionService, never()).createAndSaveAddressSession(sessionRequest);
    }

    @Test
    void shouldCatchServerExceptionAndReturn500Response()
            throws ValidationException, ServerException, JsonProcessingException {

        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");
        when(addressSessionService.validateSessionRequest("some json"))
                .thenThrow(new ServerException(new NullPointerException()));
        setupDomainProbeErrorBehaviour();

        APIGatewayProxyResponseEvent responseEvent =
                sessionHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, responseEvent.getStatusCode());
        Map responseBody = new ObjectMapper().readValue(responseEvent.getBody(), Map.class);
        assertEquals(ErrorResponse.SERVER_CONFIG_ERROR.getCode(), responseBody.get("code"));
        assertEquals(ErrorResponse.SERVER_CONFIG_ERROR.getMessage(), responseBody.get("message"));

        verify(domainProbe).counterMetric("session_created", 0d);
        verify(addressSessionService, never()).createAndSaveAddressSession(sessionRequest);
    }

    private void setupDomainProbeErrorBehaviour() {
        when(domainProbe.counterMetric(anyString(), anyDouble())).thenReturn(domainProbe);
        when(domainProbe.log(any(Level.class), any(Exception.class))).thenReturn(domainProbe);
    }
}
