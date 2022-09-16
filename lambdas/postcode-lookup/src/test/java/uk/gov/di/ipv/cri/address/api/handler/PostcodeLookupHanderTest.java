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
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostcodeLookupHanderTest {
    @Mock private PostcodeLookupService postcodeLookupService;
    @Mock private SessionService sessionService;
    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;
    @Mock private AuditService auditService;
    @Mock private EventProbe eventProbe;

    private PostcodeLookupHandler postcodeLookupHandler;

    @BeforeEach
    void setUp() {

        postcodeLookupHandler =
                new PostcodeLookupHandler(
                        postcodeLookupService, sessionService, eventProbe, auditService);
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
    void SessionErrorThrows403()
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
        assertEquals(403, responseEvent.getStatusCode());
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
    void ValidLookupReturns200() throws JsonProcessingException, SqsException {
        String testPostcode = "LS1 1BA";
        String sessionId = String.valueOf(UUID.randomUUID());
        PersonIdentityDetailed personIdentity = mock(PersonIdentityDetailed.class);
        SessionItem sessionItem = mock(SessionItem.class);
        Map<String, String> requestHeaders = Map.of("session_id", sessionId);
        AuditEventContext testAuditEventContext = mock(AuditEventContext.class);

        when(eventProbe.counterMetric(anyString())).thenReturn(eventProbe);
        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(requestHeaders);
        when(apiGatewayProxyRequestEvent.getPathParameters())
                .thenReturn(Map.of("postcode", testPostcode));
        when(postcodeLookupService.lookupPostcode(isNotNull())).thenReturn(new ArrayList<>());
        when(sessionService.validateSessionId(sessionId)).thenReturn(sessionItem);
        when(postcodeLookupService.getAuditEventContext(testPostcode, requestHeaders, sessionItem))
                .thenReturn(testAuditEventContext);

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(200, responseEvent.getStatusCode());
        verify(eventProbe).counterMetric("postcode_lookup");
        verify(sessionService).validateSessionId(sessionId);
        verify(postcodeLookupService)
                .getAuditEventContext(testPostcode, requestHeaders, sessionItem);
        verify(postcodeLookupService).lookupPostcode(testPostcode);
        verify(auditService).sendAuditEvent(AuditEventType.REQUEST_SENT, testAuditEventContext);
    }

    private void setupEventProbeErrorBehaviour() {
        when(eventProbe.counterMetric(anyString(), anyDouble())).thenReturn(eventProbe);
        when(eventProbe.log(any(Level.class), any(Exception.class))).thenReturn(eventProbe);
    }
}
