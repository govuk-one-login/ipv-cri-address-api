package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.HttpStatusCode;
import uk.gov.di.ipv.cri.address.api.exceptions.ClientIdNotSupportedException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupBadRequestException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupTimeoutException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeValidationException;
import uk.gov.di.ipv.cri.address.api.service.PostcodeLookupService;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.PostcodeLookupHandler.LAMBDA_NAME;
import static uk.gov.di.ipv.cri.address.api.handler.PostcodeLookupHandler.POSTCODE_ERROR;
import static uk.gov.di.ipv.cri.address.api.handler.PostcodeLookupHandler.POSTCODE_ERROR_MESSAGE;
import static uk.gov.di.ipv.cri.address.api.handler.PostcodeLookupHandler.POSTCODE_ERROR_TYPE;
import static uk.gov.di.ipv.cri.address.api.handler.PostcodeLookupHandler.SESSION_ID;

@ExtendWith(MockitoExtension.class)
class PostcodeLookupHandlerTest {
    private static final String TEST_POSTCODE = "LS1 1BA";
    private static final String TEST_POSTCODE_BODY = "{ \"postcode\": \"" + TEST_POSTCODE + "\" }";
    private static final String TEST_SESSION_ID = String.valueOf(UUID.randomUUID());
    private static final Map<String, String> TEST_REQUEST_HEADERS =
            Map.of(SESSION_ID, TEST_SESSION_ID);
    private static final String TEST_CLIENT_ID = "mock-client-id";

    @Mock private PostcodeLookupService postcodeLookupService;
    @Mock private SessionService sessionService;
    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;
    @Mock private AuditService auditService;
    @Mock private EventProbe eventProbe;
    @InjectMocks PostcodeLookupHandler postcodeLookupHandler;
    private ArgumentCaptor<Map<String, String>> argumentCaptorDimension =
            ArgumentCaptor.forClass(Map.class);

    @Nested
    class ValidRequests {

        @Mock private SessionItem mockSessionItem;
        @Mock private AuditEventContext mockAuditEventContext;

        @BeforeEach
        void setup() {
            when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
            when(eventProbe.counterMetric(LAMBDA_NAME)).thenReturn(eventProbe);
            when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(TEST_REQUEST_HEADERS);
            when(sessionService.validateSessionId(TEST_SESSION_ID)).thenReturn(mockSessionItem);
            when(mockSessionItem.getClientId()).thenReturn(TEST_CLIENT_ID);
            when(postcodeLookupService.getAuditEventContext(
                            TEST_POSTCODE, TEST_REQUEST_HEADERS, mockSessionItem))
                    .thenReturn(mockAuditEventContext);
        }

        @Test
        void postReturns200WithNoAddresses() throws JsonProcessingException {
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn(TEST_POSTCODE_BODY);
            when(mockSessionItem.getClientId()).thenReturn(TEST_CLIENT_ID);
            when(postcodeLookupService.lookupPostcode(TEST_POSTCODE, TEST_CLIENT_ID))
                    .thenReturn(Collections.emptyList());

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);

            assertEquals(HttpStatusCode.OK, responseEvent.getStatusCode());
            assertEquals("[]", responseEvent.getBody());
        }

        @Test
        void postReturns200WithAddresses() throws JsonProcessingException {
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn(TEST_POSTCODE_BODY);
            when(mockSessionItem.getClientId()).thenReturn(TEST_CLIENT_ID);

            CanonicalAddress address = new CanonicalAddress();
            address.setPostalCode(TEST_POSTCODE);
            address.setBuildingName("Test Address");
            when(postcodeLookupService.lookupPostcode(TEST_POSTCODE, TEST_CLIENT_ID))
                    .thenReturn(Collections.singletonList(address));

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);

            assertEquals(HttpStatusCode.OK, responseEvent.getStatusCode());
            assertEquals(
                    "[{\"buildingName\":\"Test Address\",\"postalCode\":\"LS1 1BA\"}]",
                    responseEvent.getBody());
        }

        @Test
        void returns200AndCallsServices() throws JsonProcessingException {
            when(apiGatewayProxyRequestEvent.getBody())
                    .thenReturn("{ \"postcode\": \"" + TEST_POSTCODE + "\" }");

            when(postcodeLookupService.lookupPostcode(TEST_POSTCODE, TEST_CLIENT_ID))
                    .thenReturn(Collections.emptyList());

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);

            verify(sessionService).validateSessionId(TEST_SESSION_ID);
            verify(postcodeLookupService, times(2))
                    .getAuditEventContext(TEST_POSTCODE, TEST_REQUEST_HEADERS, mockSessionItem);

            verify(postcodeLookupService).lookupPostcode(TEST_POSTCODE, TEST_CLIENT_ID);

            assertEquals(HttpStatusCode.OK, responseEvent.getStatusCode());
            assertEquals("[]", responseEvent.getBody());
        }

        @Test
        void returns200AndAuditsEvents() throws SqsException, JsonProcessingException {
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn(TEST_POSTCODE_BODY);
            when(postcodeLookupService.lookupPostcode(TEST_POSTCODE, TEST_CLIENT_ID))
                    .thenReturn(Collections.emptyList());

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);

            verify(auditService).sendAuditEvent(AuditEventType.REQUEST_SENT, mockAuditEventContext);
            verify(auditService)
                    .sendAuditEvent(AuditEventType.RESPONSE_RECEIVED, mockAuditEventContext);
            verify(eventProbe).counterMetric(LAMBDA_NAME);
            verifyNoMoreInteractions(eventProbe);

            assertEquals(HttpStatusCode.OK, responseEvent.getStatusCode());
            assertEquals("[]", responseEvent.getBody());
        }
    }

    @Nested
    class BadRequests {

        @Mock private SessionItem mockSessionItem;

        @Test
        void postReturns400WhenPostcodeBodyEmpty() throws JsonProcessingException {
            when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
            setupEventProbeExpectedErrorBehaviour();
            doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

            when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(TEST_REQUEST_HEADERS);
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn("{ \"postcode\": \"\" }");

            when(sessionService.validateSessionId(TEST_SESSION_ID)).thenReturn(mockSessionItem);
            when(mockSessionItem.getClientId()).thenReturn(TEST_CLIENT_ID);

            PostcodeValidationException exception =
                    new PostcodeValidationException("Postcode is empty");
            when(postcodeLookupService.lookupPostcode("", TEST_CLIENT_ID)).thenThrow(exception);

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
            var capturedDimension = argumentCaptorDimension.getValue();

            verifyErrorsLoggedByEventProbe(exception);
            verifyNoMoreInteractions(eventProbe);

            assertEquals(
                    Map.of(
                            POSTCODE_ERROR_TYPE,
                            "invalid_postcode_param",
                            POSTCODE_ERROR_MESSAGE,
                            "Postcode_is_empty"),
                    capturedDimension);
            assertEquals(HttpStatusCode.BAD_REQUEST, responseEvent.getStatusCode());
            assertEquals("\"Postcode is empty\"", responseEvent.getBody());
        }

        @Test
        void postReturns400WhenPostcodeBodyInvalid() {
            setupEventProbeExpectedErrorBehaviour();
            doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

            when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(TEST_REQUEST_HEADERS);
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn("{ }");

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
            var capturedDimension = argumentCaptorDimension.getValue();

            verify(eventProbe).log(eq(ERROR), any(PostcodeLookupBadRequestException.class));
            verify(eventProbe).counterMetric(POSTCODE_ERROR);
            verifyNoMoreInteractions(eventProbe);

            assertEquals(
                    Map.of(
                            POSTCODE_ERROR_TYPE,
                            "invalid_postcode_param",
                            POSTCODE_ERROR_MESSAGE,
                            "Missing_postcode_in_request_body."),
                    capturedDimension);
            assertEquals(HttpStatusCode.BAD_REQUEST, responseEvent.getStatusCode());
            assertEquals("\"Missing postcode in request body.\"", responseEvent.getBody());
        }

        @Test
        void postRequestFailsWith400ForMalformedPostcodeJson() {
            var badRequestExceptionArgumentCaptor =
                    ArgumentCaptor.forClass(PostcodeLookupBadRequestException.class);
            when(eventProbe.log(eq(ERROR), badRequestExceptionArgumentCaptor.capture()))
                    .thenReturn(eventProbe);
            when(eventProbe.counterMetric(POSTCODE_ERROR)).thenReturn(eventProbe);
            doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

            when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(TEST_REQUEST_HEADERS);
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn("SW1A 1AA");

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);

            var capturedBadRequest = badRequestExceptionArgumentCaptor.getValue();
            var capturedDimension = argumentCaptorDimension.getValue();

            verifyErrorsLoggedByEventProbe(capturedBadRequest);
            verifyNoMoreInteractions(eventProbe);

            assertEquals(
                    Map.of(
                            POSTCODE_ERROR_TYPE,
                            "invalid_postcode_param",
                            POSTCODE_ERROR_MESSAGE,
                            "Failed_to_parse_postcode_from_request_body"),
                    capturedDimension);
            assertEquals(HttpStatusCode.BAD_REQUEST, responseEvent.getStatusCode());
            assertEquals("\"Failed to parse postcode from request body\"", responseEvent.getBody());
        }

        @Test
        void sessionNotFoundReturns403() {
            when(apiGatewayProxyRequestEvent.getHeaders())
                    .thenReturn(Map.of(SESSION_ID, TEST_SESSION_ID));
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn(TEST_POSTCODE_BODY);

            SessionNotFoundException sessionNotFoundException =
                    new SessionNotFoundException("Session not found");
            when(sessionService.validateSessionId(TEST_SESSION_ID))
                    .thenThrow(sessionNotFoundException);

            setupEventProbeExpectedErrorBehaviour();
            doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
            var capturedDimension = argumentCaptorDimension.getValue();

            verify(eventProbe).addDimensions(capturedDimension);
            verifyErrorsLoggedByEventProbe(sessionNotFoundException);
            verifyNoMoreInteractions(eventProbe);

            assertEquals(
                    Map.of(
                            POSTCODE_ERROR_TYPE,
                            "session_not_found",
                            POSTCODE_ERROR_MESSAGE,
                            "Session_not_found"),
                    capturedDimension);
            assertEquals(HttpStatusCode.FORBIDDEN, responseEvent.getStatusCode());
            assertEquals(
                    "{\"error_description\":\"Access denied by resource owner or authorization server - Session not found\",\"error\":\"access_denied\"}",
                    responseEvent.getBody());
        }

        @Test
        void sessionExpiredReturns403() {
            when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(TEST_REQUEST_HEADERS);
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn(TEST_POSTCODE_BODY);

            SessionExpiredException sessionExpiredException =
                    new SessionExpiredException("session expired");
            when(sessionService.validateSessionId(TEST_SESSION_ID))
                    .thenThrow(sessionExpiredException);

            setupEventProbeExpectedErrorBehaviour();
            doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
            var capturedDimension = argumentCaptorDimension.getValue();

            verify(eventProbe).addDimensions(argumentCaptorDimension.getValue());
            verifyErrorsLoggedByEventProbe(sessionExpiredException);
            verifyNoMoreInteractions(eventProbe);

            assertEquals(
                    Map.of(
                            POSTCODE_ERROR_TYPE,
                            "session_expired",
                            POSTCODE_ERROR_MESSAGE,
                            "session_expired"),
                    capturedDimension);
            assertEquals(HttpStatusCode.FORBIDDEN, responseEvent.getStatusCode());
            assertEquals(
                    "{\"error_description\":\"Access denied by resource owner or authorization server - Session expired\",\"error\":\"access_denied\"}",
                    responseEvent.getBody());
        }

        @Test
        void clientIdNotFoundReturns400() throws JsonProcessingException {
            var badRequestExceptionArgumentCaptor =
                    ArgumentCaptor.forClass(ClientIdNotSupportedException.class);
            when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
            when(eventProbe.log(eq(ERROR), badRequestExceptionArgumentCaptor.capture()))
                    .thenReturn(eventProbe);
            when(eventProbe.counterMetric(POSTCODE_ERROR)).thenReturn(eventProbe);
            doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

            when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(TEST_REQUEST_HEADERS);
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn(TEST_POSTCODE_BODY);

            when(sessionService.validateSessionId(TEST_SESSION_ID)).thenReturn(mockSessionItem);
            when(mockSessionItem.getClientId()).thenReturn(TEST_CLIENT_ID);

            ClientIdNotSupportedException exception =
                    new ClientIdNotSupportedException(
                            "The Client ID provided for this session is not supported");
            when(postcodeLookupService.lookupPostcode(TEST_POSTCODE, TEST_CLIENT_ID))
                    .thenThrow(exception);

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);

            var capturedBadRequest = badRequestExceptionArgumentCaptor.getValue();
            var capturedDimension = argumentCaptorDimension.getValue();

            verifyErrorsLoggedByEventProbe(capturedBadRequest);
            verifyNoMoreInteractions(eventProbe);

            assertEquals(
                    Map.of(
                            POSTCODE_ERROR_TYPE,
                            "lookup_server",
                            POSTCODE_ERROR_MESSAGE,
                            "The_Client_ID_provided_for_this_session_is_not_supported"),
                    capturedDimension);
            assertEquals(HttpStatusCode.BAD_REQUEST, responseEvent.getStatusCode());
            assertEquals(
                    "\"The Client ID provided for this session is not supported\"",
                    responseEvent.getBody());
        }
    }

    @Nested
    class ServiceErrors {

        @Mock private SessionItem mockSessionItem;

        @BeforeEach
        void setup() {
            when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(TEST_REQUEST_HEADERS);
            when(apiGatewayProxyRequestEvent.getBody()).thenReturn(TEST_POSTCODE_BODY);
        }

        @Test
        void runtimeSessionServiceExceptionReturns401() {
            RuntimeException exception = new RuntimeException("Any other exception");

            setupEventProbeExpectedErrorBehaviour();
            doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

            when(sessionService.validateSessionId(anyString())).thenThrow(exception);

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
            var dimension = argumentCaptorDimension.getValue();

            verifyErrorsLoggedByEventProbe(exception);
            verifyNoMoreInteractions(eventProbe);
            assertEquals(
                    Map.of(
                            POSTCODE_ERROR_TYPE,
                            "lookup_server",
                            POSTCODE_ERROR_MESSAGE,
                            "Any_other_exception"),
                    dimension);
            assertEquals(HttpStatusCode.UNAUTHORIZED, responseEvent.getStatusCode());
            assertEquals("\"Any other exception\"", responseEvent.getBody().toString());
        }

        @Test
        void postcodeLookupServiceTimeoutReturns408() throws JsonProcessingException {
            AuditEventContext testAuditEventContext = mock(AuditEventContext.class);

            when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
            setupEventProbeExpectedErrorBehaviour();
            doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

            when(sessionService.validateSessionId(TEST_SESSION_ID)).thenReturn(mockSessionItem);
            when(mockSessionItem.getClientId()).thenReturn(TEST_CLIENT_ID);
            when(postcodeLookupService.getAuditEventContext(
                            TEST_POSTCODE, TEST_REQUEST_HEADERS, mockSessionItem))
                    .thenReturn(testAuditEventContext);

            PostcodeLookupTimeoutException exception =
                    new PostcodeLookupTimeoutException("Error Connection Timeout");
            when(postcodeLookupService.lookupPostcode(TEST_POSTCODE, TEST_CLIENT_ID))
                    .thenThrow(exception);

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
            var dimension = argumentCaptorDimension.getValue();

            verify(eventProbe).addDimensions(dimension);
            verifyErrorsLoggedByEventProbe(exception);
            verifyNoMoreInteractions(eventProbe);

            assertEquals(HttpStatusCode.REQUEST_TIMEOUT, responseEvent.getStatusCode());
            assertEquals(
                    Map.of(
                            POSTCODE_ERROR_TYPE,
                            "time_out_error",
                            POSTCODE_ERROR_MESSAGE,
                            "Error_Connection_Timeout"),
                    dimension);
            assertEquals("\"Error Connection Timeout\"", responseEvent.getBody().toString());
        }

        @Test
        void postcodeLookupServiceProcessingExceptionReturns404() throws JsonProcessingException {
            AuditEventContext testAuditEventContext = mock(AuditEventContext.class);

            when(eventProbe.log(INFO, "found session")).thenReturn(eventProbe);
            setupEventProbeExpectedErrorBehaviour();
            doNothing().when(eventProbe).addDimensions(argumentCaptorDimension.capture());

            when(sessionService.validateSessionId(TEST_SESSION_ID)).thenReturn(mockSessionItem);
            when(mockSessionItem.getClientId()).thenReturn(TEST_CLIENT_ID);
            when(postcodeLookupService.getAuditEventContext(
                            TEST_POSTCODE, TEST_REQUEST_HEADERS, mockSessionItem))
                    .thenReturn(testAuditEventContext);

            PostcodeLookupProcessingException exception =
                    new PostcodeLookupProcessingException(
                            "Error sending request for postcode lookup");
            when(postcodeLookupService.lookupPostcode(TEST_POSTCODE, TEST_CLIENT_ID))
                    .thenThrow(exception);

            APIGatewayProxyResponseEvent responseEvent =
                    postcodeLookupHandler.handleRequest(apiGatewayProxyRequestEvent, null);
            var dimension = argumentCaptorDimension.getValue();

            verifyErrorsLoggedByEventProbe(exception);
            verifyNoMoreInteractions(eventProbe);
            verify(eventProbe).addDimensions(dimension);

            assertEquals(HttpStatusCode.NOT_FOUND, responseEvent.getStatusCode());
            assertEquals(
                    Map.of(
                            POSTCODE_ERROR_TYPE,
                            "lookup_processing",
                            POSTCODE_ERROR_MESSAGE,
                            "Error_sending_request_for_postcode_lookup"),
                    dimension);
            assertEquals("\"Error sending request for postcode lookup\"", responseEvent.getBody());
        }
    }

    private void setupEventProbeExpectedErrorBehaviour() {
        when(eventProbe.log(eq(ERROR), any(Exception.class))).thenReturn(eventProbe);
        when(eventProbe.counterMetric(POSTCODE_ERROR)).thenReturn(eventProbe);
    }

    private void verifyErrorsLoggedByEventProbe(Exception exception) {
        verify(eventProbe).log(ERROR, exception);
        verify(eventProbe).counterMetric(POSTCODE_ERROR);
    }
}
