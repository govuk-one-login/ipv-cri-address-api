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
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.models.AuthorizationResponse;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressHandlerTest {

    @Mock private AddressSessionService addressSessionService;

    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;

    @Mock private EventProbe eventProbe;

    private AddressHandler addressHandler;

    @BeforeEach
    void setUp() {

        addressHandler = new AddressHandler(addressSessionService, eventProbe);
    }

    @Test
    void SessionValidationReturns400()
            throws SessionExpiredException, SessionNotFoundException, AddressProcessingException {

        setupEventProbeErrorBehaviour();

        SessionNotFoundException exception = new SessionNotFoundException("Session not found");

        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");

        List<CanonicalAddress> canonicalAddresses = new ArrayList<>();
        CanonicalAddress canonicalAddress = new CanonicalAddress();
        canonicalAddress.setUprn(Long.valueOf("12345"));
        canonicalAddresses.add(canonicalAddress);

        when(addressSessionService.parseAddresses(anyString())).thenReturn(canonicalAddresses);
        when(addressSessionService.saveAddresses(notNull(), anyList())).thenThrow(exception);

        APIGatewayProxyResponseEvent responseEvent =
                addressHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(400, responseEvent.getStatusCode());
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric("address", 0d);
    }

    @Test
    void ValidSaveReturnsAuthorizationCode()
            throws JsonProcessingException, AddressProcessingException, SessionExpiredException,
                    SessionValidationException, SessionNotFoundException {

        when(eventProbe.counterMetric(anyString())).thenReturn(eventProbe);

        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");
        AddressSessionItem addressSessionItem = new AddressSessionItem();

        List<CanonicalAddress> canonicalAddresses = new ArrayList<>();
        CanonicalAddress canonicalAddress = new CanonicalAddress();
        canonicalAddress.setUprn(Long.valueOf("12345"));
        canonicalAddresses.add(canonicalAddress);
        addressSessionItem.setAddresses(canonicalAddresses);
        AuthorizationResponse authorizationResponse = new AuthorizationResponse(addressSessionItem);

        when(addressSessionService.parseAddresses(anyString())).thenReturn(canonicalAddresses);
        when(addressSessionService.saveAddresses(notNull(), anyList()))
                .thenReturn(addressSessionItem);

        APIGatewayProxyResponseEvent responseEvent =
                addressHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(201, responseEvent.getStatusCode());

        assertEquals(
                responseEvent.getBody(),
                new ObjectMapper().writeValueAsString(authorizationResponse));
        verify(eventProbe).counterMetric("address");
    }

    @Test
    void EmptyAddressesReturns200() throws AddressProcessingException {

        when(apiGatewayProxyRequestEvent.getHeaders())
                .thenReturn(Map.of("session_id", UUID.randomUUID().toString()));
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");

        List<CanonicalAddress> canonicalAddresses = new ArrayList<>();

        when(addressSessionService.parseAddresses(anyString())).thenReturn(canonicalAddresses);

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
