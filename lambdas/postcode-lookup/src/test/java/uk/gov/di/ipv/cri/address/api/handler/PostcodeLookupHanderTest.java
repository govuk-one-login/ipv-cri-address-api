package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupValidationException;
import uk.gov.di.ipv.cri.address.api.service.PostcodeLookupService;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.service.SessionService;
import uk.gov.di.ipv.cri.address.library.util.EventProbe;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostcodeLookupHanderTest {

    private static Instant fixedInstant;

    @Mock private PostcodeLookupService postcodeLookupService;
    @Mock private SessionService sessionService;

    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;

    @Mock private EventProbe eventProbe;

    private PostcodeLookupHandler postcodeLookupHandler;

    @BeforeEach
    void setUp() {

        postcodeLookupHandler =
                new PostcodeLookupHandler(postcodeLookupService, sessionService, eventProbe);
        fixedInstant = Instant.now();
    }

    @Test
    void PostcodeLookupValidationExceptionReturns400()
            throws JsonProcessingException, PostcodeLookupValidationException,
                    PostcodeLookupProcessingException {

        setupEventProbeErrorBehaviour();

        PostcodeLookupValidationException exception =
                new PostcodeLookupValidationException("Postcode is empty");

        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(new HashMap<>());

        when(postcodeLookupService.lookupPostcode(isNull())).thenThrow(exception);

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(400, responseEvent.getStatusCode());
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric("postcode_lookup", 0d);
    }

    @Test
    void SessionErrorThrows400()
            throws PostcodeLookupValidationException, PostcodeLookupProcessingException,
                    SessionExpiredException, SessionNotFoundException {

        setupEventProbeErrorBehaviour();

        SessionNotFoundException exception = new SessionNotFoundException("Session not found");

        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(Map.of("postcode", ""));
        willThrow(exception).given(sessionService).validateSessionId(anyString());

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(400, responseEvent.getStatusCode());
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric("postcode_lookup", 0d);
    }

    @Test
    void AnyOtherExceptionThrows500()
            throws PostcodeLookupValidationException, PostcodeLookupProcessingException,
                    SessionExpiredException, SessionValidationException, SessionNotFoundException {

        setupEventProbeErrorBehaviour();

        RuntimeException exception = new RuntimeException("Any other exception");

        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(Map.of("postcode", ""));
        willThrow(exception).given(sessionService).validateSessionId(anyString());

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(500, responseEvent.getStatusCode());
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric("postcode_lookup", 0d);
    }

    @Test
    void ValidLookupReturns200() throws JsonProcessingException {

        when(eventProbe.counterMetric(anyString())).thenReturn(eventProbe);

        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(Map.of("postcode", ""));

        when(postcodeLookupService.lookupPostcode(isNotNull())).thenReturn(new ArrayList<>());

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(200, responseEvent.getStatusCode());
        verify(eventProbe).counterMetric("postcode_lookup");
    }

    private void setupEventProbeErrorBehaviour() {
        when(eventProbe.counterMetric(anyString(), anyDouble())).thenReturn(eventProbe);
        when(eventProbe.log(any(Level.class), any(Exception.class))).thenReturn(eventProbe);
    }
}
