package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.HttpStatusCode;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;
import uk.gov.di.ipv.cri.address.library.service.AddressService;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressHandlerTest {
    private static final String SESSION_ID = UUID.randomUUID().toString();
    private static final long ADDRESS_TTL = 1730197212;

    @Mock private SessionService mockSessionService;
    @Mock private AddressService mockAddressService;

    @Mock private APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;

    @Mock private EventProbe eventProbe;

    private AddressHandler addressHandler;

    @BeforeEach
    void setUp() {
        addressHandler = new AddressHandler(mockSessionService, mockAddressService, eventProbe);
    }

    @Test
    void SessionValidationReturns400()
            throws SessionExpiredException, SessionNotFoundException, AddressProcessingException {

        setupEventProbeExpectedErrorBehaviour();

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
        assertEquals(403, responseEvent.getStatusCode());
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric("address", 0d);
        verifyNoMoreInteractions(eventProbe);
    }

    @Test
    void ValidSaveGeneratesAuthorizationCode()
            throws AddressProcessingException, SessionExpiredException, SessionNotFoundException {

        when(eventProbe.counterMetric(anyString())).thenReturn(eventProbe);
        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(Map.of("session_id", SESSION_ID));
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");
        SessionItem sessionItem = new SessionItem();

        List<CanonicalAddress> canonicalAddresses = new ArrayList<>();
        CanonicalAddress canonicalAddress = new CanonicalAddress();
        canonicalAddress.setValidFrom(LocalDate.of(2013, 8, 9));
        canonicalAddress.setUprn(Long.valueOf("12345"));
        canonicalAddresses.add(canonicalAddress);

        AddressItem addressItem = new AddressItem();
        addressItem.setAddresses(canonicalAddresses);
        addressItem.setExpiryDate(ADDRESS_TTL);

        when(mockSessionService.validateSessionId(SESSION_ID)).thenReturn(sessionItem);
        when(mockAddressService.parseAddresses(anyString())).thenReturn(canonicalAddresses);

        APIGatewayProxyResponseEvent responseEvent =
                addressHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(HttpStatusCode.NO_CONTENT, responseEvent.getStatusCode());

        verify(mockSessionService).createAuthorizationCode(sessionItem);
        verify(mockAddressService).setAddressValidity(canonicalAddresses);
        verify(eventProbe).log(Level.INFO, "found session");
        verify(eventProbe).counterMetric("address");
        verifyNoMoreInteractions(eventProbe);
    }

    @Test
    void EmptyAddressesReturns200() throws AddressProcessingException {

        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(Map.of("session_id", SESSION_ID));
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");

        List<CanonicalAddress> canonicalAddresses = new ArrayList<>();

        when(mockAddressService.parseAddresses(anyString())).thenReturn(canonicalAddresses);

        APIGatewayProxyResponseEvent responseEvent =
                addressHandler.handleRequest(apiGatewayProxyRequestEvent, null);
        assertEquals(HttpStatusCode.OK, responseEvent.getStatusCode());

        verifyNoInteractions(eventProbe);
    }

    @Test
    void shouldReturn500WhenParseAddressesThrowsAddressProcessingException()
            throws AddressProcessingException {
        setupEventProbeExpectedErrorBehaviour();
        AddressProcessingException addressProcessingException =
                new AddressProcessingException("Country code not present for address");

        when(apiGatewayProxyRequestEvent.getHeaders()).thenReturn(Map.of("session_id", SESSION_ID));
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("some json");
        when(mockAddressService.parseAddresses(anyString())).thenThrow(addressProcessingException);

        APIGatewayProxyResponseEvent responseEvent =
                addressHandler.handleRequest(apiGatewayProxyRequestEvent, null);

        assertEquals(500, responseEvent.getStatusCode());
        verify(eventProbe).log(Level.ERROR, addressProcessingException);
        verify(eventProbe).counterMetric("address", 0d);
        verifyNoMoreInteractions(eventProbe);
    }

    private void setupEventProbeExpectedErrorBehaviour() {
        when(eventProbe.log(eq(Level.ERROR), Mockito.any(Exception.class))).thenReturn(eventProbe);
        when(eventProbe.counterMetric("address", 0d)).thenReturn(eventProbe);
    }
}
