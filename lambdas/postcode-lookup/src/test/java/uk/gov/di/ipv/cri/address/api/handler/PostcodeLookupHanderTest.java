package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.PostcodeLookupHandler.LAMBDA_NAME;

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

        when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
        setupEventProbeExpectedErrorBehaviour();

        PostcodeLookupValidationException exception =
                new PostcodeLookupValidationException("Postcode is empty");

        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(new HashMap<>());

        when(postcodeLookupService.lookupPostcode(isNull())).thenThrow(exception);

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(400, responseEvent.getStatusCode());
        verifyErrorsLoggedByEventProbe(exception);
        verifyNoMoreInteractions(eventProbe);
    }

    @Test
    void SessionErrorThrows403()
            throws PostcodeLookupValidationException, PostcodeLookupProcessingException,
                    SessionExpiredException, SessionNotFoundException {

        setupEventProbeExpectedErrorBehaviour();

        SessionNotFoundException exception = new SessionNotFoundException("Session not found");

        String sessionId = UUID.randomUUID().toString();
        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(Map.of("session_id", sessionId));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(Map.of("postcode", ""));
        willThrow(exception).given(sessionService).validateSessionId(sessionId);

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(403, responseEvent.getStatusCode());
        verifyErrorsLoggedByEventProbe(exception);
        verifyNoMoreInteractions(eventProbe);
    }

    @Test
    void AnyOtherExceptionThrows500()
            throws PostcodeLookupValidationException, PostcodeLookupProcessingException,
                    SessionExpiredException, SessionValidationException, SessionNotFoundException {

        setupEventProbeExpectedErrorBehaviour();

        RuntimeException exception = new RuntimeException("Any other exception");

        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(Map.of("postcode", ""));
        willThrow(exception).given(sessionService).validateSessionId(anyString());

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(500, responseEvent.getStatusCode());
        verifyErrorsLoggedByEventProbe(exception);
        verifyNoMoreInteractions(eventProbe);
    }

    @Test
    void ValidLookupReturns200() throws JsonProcessingException, SqsException {
        String testPostcode = "LS1 1BA";
        String sessionId = String.valueOf(UUID.randomUUID());
        PersonIdentityDetailed personIdentity = mock(PersonIdentityDetailed.class);
        SessionItem sessionItem = mock(SessionItem.class);
        Map<String, String> requestHeaders = Map.of("session_id", sessionId);
        AuditEventContext testAuditEventContext = mock(AuditEventContext.class);

        when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
        when(eventProbe.counterMetric(LAMBDA_NAME)).thenReturn(eventProbe);
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
        verify(sessionService).validateSessionId(sessionId);
        verify(postcodeLookupService)
                .getAuditEventContext(testPostcode, requestHeaders, sessionItem);
        verify(postcodeLookupService).lookupPostcode(testPostcode);
        verify(auditService).sendAuditEvent(AuditEventType.REQUEST_SENT, testAuditEventContext);
        verify(eventProbe).counterMetric(LAMBDA_NAME);
        verifyNoMoreInteractions(eventProbe);
    }

    private void setupEventProbeExpectedErrorBehaviour() {
        when(eventProbe.log(eq(Level.ERROR), Mockito.any(Exception.class))).thenReturn(eventProbe);
        when(eventProbe.counterMetric(LAMBDA_NAME, 0d)).thenReturn(eventProbe);
    }

    private void verifyErrorsLoggedByEventProbe(Exception exception) {
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric(LAMBDA_NAME, 0d);
    }
}
