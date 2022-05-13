package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.address.library.domain.AuthorizationResponse;
import uk.gov.di.ipv.cri.address.library.domain.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.SqsException;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;
import uk.gov.di.ipv.cri.address.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.address.library.service.AddressService;
import uk.gov.di.ipv.cri.address.library.service.AuditService;
import uk.gov.di.ipv.cri.address.library.service.SessionService;
import uk.gov.di.ipv.cri.address.library.util.EventProbe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressHandlerTest {
    private static final String SESSION_ID = UUID.randomUUID().toString();
    @Mock private SessionService mockSessionService;
    @Mock private AddressService mockAddressService;

    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;

    @Mock private EventProbe eventProbe;

    @Mock private AuditService auditService;

    private AddressHandler addressHandler;

    @BeforeEach
    void setUp() {
        addressHandler =
                new AddressHandler(
                        mockSessionService, mockAddressService, eventProbe, auditService);
    }

    @Test
    void SessionValidationReturns400()
            throws SessionExpiredException, SessionNotFoundException, AddressProcessingException {

        setupEventProbeErrorBehaviour();

        SessionNotFoundException exception = new SessionNotFoundException("Session not found");
        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(Map.of("session_id", SESSION_ID));
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");

        List<CanonicalAddress> canonicalAddresses = new ArrayList<>();
        CanonicalAddress canonicalAddress = new CanonicalAddress();
        canonicalAddress.setUprn(Long.valueOf("12345"));
        canonicalAddresses.add(canonicalAddress);

        when(mockAddressService.parseAddresses(anyString())).thenReturn(canonicalAddresses);
        when(mockSessionService.validateSessionId(SESSION_ID)).thenThrow(exception);

        APIGatewayProxyResponseEvent responseEvent =
                addressHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(400, responseEvent.getStatusCode());
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric("address", 0d);
    }

    @Test
    void ValidSaveReturnsAuthorizationCode()
            throws JsonProcessingException, AddressProcessingException, SessionExpiredException,
                    SessionNotFoundException, SqsException {

        when(eventProbe.counterMetric(anyString())).thenReturn(eventProbe);
        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(Map.of("session_id", SESSION_ID));
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");
        SessionItem sessionItem = new SessionItem();

        List<CanonicalAddress> canonicalAddresses = new ArrayList<>();
        CanonicalAddress canonicalAddress = new CanonicalAddress();
        canonicalAddress.setUprn(Long.valueOf("12345"));
        canonicalAddresses.add(canonicalAddress);
        AuthorizationResponse authorizationResponse = new AuthorizationResponse(sessionItem);

        AddressItem addressItem = new AddressItem();
        addressItem.setAddresses(canonicalAddresses);

        when(mockSessionService.validateSessionId(SESSION_ID)).thenReturn(sessionItem);
        when(mockAddressService.parseAddresses(anyString())).thenReturn(canonicalAddresses);
        when(mockAddressService.saveAddresses(notNull(), anyList())).thenReturn(addressItem);

        APIGatewayProxyResponseEvent responseEvent =
                addressHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(201, responseEvent.getStatusCode());

        assertEquals(
                responseEvent.getBody(),
                new ObjectMapper().writeValueAsString(authorizationResponse));

        verify(mockSessionService).createAuthorizationCode(sessionItem);
        verify(eventProbe).counterMetric("address");
        verify(auditService).sendAuditEvent(AuditEventTypes.IPV_ADDRESS_CRI_REQUEST_SENT);
    }

    @Test
    void EmptyAddressesReturns200() throws AddressProcessingException {

        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(Map.of("session_id", SESSION_ID));
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");

        List<CanonicalAddress> canonicalAddresses = new ArrayList<>();

        when(mockAddressService.parseAddresses(anyString())).thenReturn(canonicalAddresses);

        APIGatewayProxyResponseEvent responseEvent =
                addressHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(200, responseEvent.getStatusCode());

        verifyNoInteractions(eventProbe);
    }

    private void setupEventProbeErrorBehaviour() {
        when(eventProbe.counterMetric(anyString(), anyDouble())).thenReturn(eventProbe);
        when(eventProbe.log(any(Level.class), any(Exception.class))).thenReturn(eventProbe);
    }
}
