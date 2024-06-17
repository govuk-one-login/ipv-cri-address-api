package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.HttpStatusCode;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupTimeoutException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeValidationException;
import uk.gov.di.ipv.cri.address.api.service.PostcodeLookupService;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.PostcodeLookupHandler.LAMBDA_NAME;
import static uk.gov.di.ipv.cri.address.api.handler.PostcodeLookupHandler.POSTCODE_ERROR;
import static uk.gov.di.ipv.cri.address.api.handler.PostcodeLookupHandler.SESSION_ID;

@ExtendWith(MockitoExtension.class)
class PostcodeLookupHanderTest {
    @Mock private PostcodeLookupService postcodeLookupService;
    @Mock private SessionService sessionService;
    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;
    @Mock private AuditService auditService;
    @Mock private EventProbe eventProbe;
    @InjectMocks PostcodeLookupHandler postcodeLookupHandler;
    private ArgumentCaptor<Map<String, String>> argumentCaptorDimension =
            ArgumentCaptor.forClass(Map.class);

    @Test
    void postcodeLookupValidationExceptionReturns400()
            throws JsonProcessingException, PostcodeValidationException,
                    PostcodeLookupProcessingException {

        PostcodeValidationException exception =
                new PostcodeValidationException("Postcode is empty");

        when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
        setupEventProbeExpectedErrorBehaviour();
        doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());
        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(new HashMap<>());
        when(postcodeLookupService.lookupPostcode(isNull())).thenThrow(exception);

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        var capturedDimension = argumentCaptorDimension.getValue();

        verifyErrorsLoggedByEventProbe(exception);
        verifyNoMoreInteractions(eventProbe);

        assertEquals(Map.of("invalid_postcode_param", "Postcode is empty"), capturedDimension);
        assertEquals(HttpStatusCode.BAD_REQUEST, responseEvent.getStatusCode());
        assertEquals("\"Postcode is empty\"", responseEvent.getBody());
    }

    @Test
    void sessionErrorThrows403()
            throws PostcodeValidationException, PostcodeLookupProcessingException,
                    SessionExpiredException, SessionNotFoundException {

        String sessionId = UUID.randomUUID().toString();
        SessionNotFoundException sessionNotFoundException =
                new SessionNotFoundException("Session not found");

        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(Map.of(SESSION_ID, sessionId));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(Map.of("postcode", ""));
        when(sessionService.validateSessionId(sessionId)).thenThrow(sessionNotFoundException);

        setupEventProbeExpectedErrorBehaviour();
        doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        var capturedDimension = argumentCaptorDimension.getValue();

        verify(eventProbe).addDimensions(capturedDimension);
        verifyErrorsLoggedByEventProbe(sessionNotFoundException);
        verifyNoMoreInteractions(eventProbe);

        assertEquals(Map.of("session_not_found", "Session not found"), capturedDimension);
        assertEquals(HttpStatusCode.FORBIDDEN, responseEvent.getStatusCode());
        assertEquals(
                "{\"error_description\":\"Access denied by resource owner or authorization server - Session not found\",\"error\":\"access_denied\"}",
                responseEvent.getBody());
    }

    @Test
    void sessionExpiredErrorThrows403()
            throws PostcodeValidationException, PostcodeLookupProcessingException,
                    SessionExpiredException, SessionNotFoundException {

        String sessionId = UUID.randomUUID().toString();
        SessionExpiredException sessionExpiredException =
                new SessionExpiredException("session expired");

        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(Map.of(SESSION_ID, sessionId));
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(Map.of("postcode", ""));
        when(sessionService.validateSessionId(sessionId)).thenThrow(sessionExpiredException);

        setupEventProbeExpectedErrorBehaviour();
        doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        var capturedDimension = argumentCaptorDimension.getValue();

        verify(eventProbe).addDimensions(argumentCaptorDimension.getValue());
        verifyErrorsLoggedByEventProbe(sessionExpiredException);
        verifyNoMoreInteractions(eventProbe);

        assertEquals(Map.of("session_expired", "session expired"), capturedDimension);
        assertEquals(HttpStatusCode.FORBIDDEN, responseEvent.getStatusCode());
        assertEquals(
                "{\"error_description\":\"Access denied by resource owner or authorization server - Session expired\",\"error\":\"access_denied\"}",
                responseEvent.getBody());
    }

    @Test
    void anyOtherExceptionThrows401()
            throws PostcodeValidationException, PostcodeLookupProcessingException,
                    SessionExpiredException, SessionValidationException, SessionNotFoundException {
        String sessionId = String.valueOf(UUID.randomUUID());
        Map<String, String> requestHeaders = Map.of("session_id", sessionId);
        RuntimeException exception = new RuntimeException("Any other exception");

        setupEventProbeExpectedErrorBehaviour();
        doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());
        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(requestHeaders);
        when(apiGatewayProxyRequestEvent.getPathParameters()).thenReturn(Map.of("postcode", ""));
        when(sessionService.validateSessionId(anyString())).thenThrow(exception);

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        var dimension = argumentCaptorDimension.getValue();

        verifyErrorsLoggedByEventProbe(exception);
        verifyNoMoreInteractions(eventProbe);
        assertEquals(Map.of("lookup_server", "Any other exception"), dimension);
        assertEquals(HttpStatusCode.UNAUTHORIZED, responseEvent.getStatusCode());
        assertEquals("\"Any other exception\"", responseEvent.getBody().toString());
    }

    @Test
    void postcodeLookupTimeoutExceptionThrowsRequestTimeOut()
            throws PostcodeValidationException, PostcodeLookupProcessingException,
                    SessionExpiredException, SessionValidationException, SessionNotFoundException,
                    JsonProcessingException {
        String testPostcode = "LS1 1BA";
        String sessionId = String.valueOf(UUID.randomUUID());
        Map<String, String> requestHeaders = Map.of("session_id", sessionId);
        SessionItem sessionItem = mock(SessionItem.class);
        AuditEventContext testAuditEventContext = mock(AuditEventContext.class);

        PostcodeLookupTimeoutException exception =
                new PostcodeLookupTimeoutException("Error Connection Timeout");

        when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
        setupEventProbeExpectedErrorBehaviour();
        doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());
        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(requestHeaders);
        when(apiGatewayProxyRequestEvent.getPathParameters())
                .thenReturn(Map.of("postcode", testPostcode));
        when(sessionService.validateSessionId(sessionId)).thenReturn(sessionItem);
        when(postcodeLookupService.getAuditEventContext(testPostcode, requestHeaders, sessionItem))
                .thenReturn(testAuditEventContext);

        doThrow(exception).when(postcodeLookupService).lookupPostcode(anyString());
        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        var dimension = argumentCaptorDimension.getValue();

        verify(eventProbe).addDimensions(dimension);
        verifyErrorsLoggedByEventProbe(exception);
        verifyNoMoreInteractions(eventProbe);

        assertEquals(HttpStatusCode.REQUEST_TIMEOUT, responseEvent.getStatusCode());
        assertEquals(Map.of("time_out_error", "Error Connection Timeout"), dimension);
        assertEquals("\"Error Connection Timeout\"", responseEvent.getBody().toString());
    }

    @Test
    void validLookupReturns200() throws JsonProcessingException, SqsException {
        String testPostcode = "LS1 1BA";
        String sessionId = String.valueOf(UUID.randomUUID());
        SessionItem sessionItem = mock(SessionItem.class);
        Map<String, String> requestHeaders = Map.of("session_id", sessionId);
        AuditEventContext testAuditEventContext = mock(AuditEventContext.class);

        when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
        when(eventProbe.counterMetric(LAMBDA_NAME)).thenReturn(eventProbe);
        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(requestHeaders);
        when(apiGatewayProxyRequestEvent.getPathParameters())
                .thenReturn(Map.of("postcode", testPostcode));
        when(postcodeLookupService.lookupPostcode(isNotNull())).thenReturn(Collections.emptyList());
        when(sessionService.validateSessionId(sessionId)).thenReturn(sessionItem);
        when(postcodeLookupService.getAuditEventContext(testPostcode, requestHeaders, sessionItem))
                .thenReturn(testAuditEventContext);

        APIGatewayProxyResponseEvent responseEvent =
                postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        verify(sessionService).validateSessionId(sessionId);
        verify(postcodeLookupService, times(2))
                .getAuditEventContext(testPostcode, requestHeaders, sessionItem);
        verify(postcodeLookupService).lookupPostcode(testPostcode);
        verify(auditService).sendAuditEvent(AuditEventType.REQUEST_SENT, testAuditEventContext);
        verify(auditService)
                .sendAuditEvent(AuditEventType.RESPONSE_RECEIVED, testAuditEventContext);
        verify(eventProbe).counterMetric(LAMBDA_NAME);
        verifyNoMoreInteractions(eventProbe);

        assertEquals(HttpStatusCode.OK, responseEvent.getStatusCode());
        assertEquals("[]", responseEvent.getBody());
    }

    private void setupEventProbeExpectedErrorBehaviour() {
        when(eventProbe.log(eq(Level.ERROR), Mockito.any(Exception.class))).thenReturn(eventProbe);
        when(eventProbe.counterMetric(POSTCODE_ERROR)).thenReturn(eventProbe);
    }

    private void verifyErrorsLoggedByEventProbe(Exception exception) {
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric(POSTCODE_ERROR);
    }
}
