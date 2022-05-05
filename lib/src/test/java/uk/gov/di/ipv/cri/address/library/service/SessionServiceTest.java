package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.address.library.util.ListUtil;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
    private static final String SESSION_ID = UUID.randomUUID().toString();
    private static Instant fixedInstant;
    private SessionService sessionService;

    @Mock private DataStore<SessionItem> mockDataStore;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private ListUtil mockListUtil;
    @Captor private ArgumentCaptor<SessionItem> sessionItemArgumentCaptor;

    @BeforeAll
    static void beforeAll() {
        fixedInstant = Instant.now();
    }

    @BeforeEach
    void setUp() {
        Clock nowClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        sessionService =
                new SessionService(mockDataStore, mockConfigurationService, nowClock, mockListUtil);
    }

    @Test
    void shouldCallCreateOnAddressSessionDataStore() {
        when(mockConfigurationService.getSessionTtl()).thenReturn(1L);
        SessionRequest sessionRequest = mock(SessionRequest.class);

        when(sessionRequest.getClientId()).thenReturn("a client id");
        when(sessionRequest.getState()).thenReturn("state");
        when(sessionRequest.getRedirectUri())
                .thenReturn(URI.create("https://www.example.com/callback"));
        when(sessionRequest.getSubject()).thenReturn("a subject");

        sessionService.createAndSaveAddressSession(sessionRequest);
        verify(mockDataStore).create(sessionItemArgumentCaptor.capture());
        SessionItem capturedValue = sessionItemArgumentCaptor.getValue();
        assertNotNull(capturedValue.getSessionId());
        assertThat(capturedValue.getExpiryDate(), equalTo(fixedInstant.getEpochSecond() + 1));
        assertThat(capturedValue.getClientId(), equalTo("a client id"));
        assertThat(capturedValue.getState(), equalTo("state"));
        assertThat(capturedValue.getSubject(), equalTo("a subject"));
        assertThat(
                capturedValue.getRedirectUri(),
                equalTo(URI.create("https://www.example.com/callback")));
    }

    @Test
    void shouldGetAddressSessionItemByAuthorizationCodeIndexSuccessfully() {
        String authCodeValue = UUID.randomUUID().toString();
        SessionItem item = new SessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAuthorizationCode(authCodeValue);
        List<SessionItem> items = List.of(item);

        when(mockListUtil.getOneItemOrThrowError(items)).thenReturn(item);
        when(mockDataStore.getItemByIndex(SessionItem.AUTHORIZATION_CODE_INDEX, authCodeValue))
                .thenReturn(items);
        when(mockDataStore.getItem(item.getSessionId().toString())).thenReturn(item);

        SessionItem sessionItem = sessionService.getSessionByAuthorisationCode(authCodeValue);
        assertThat(item.getSessionId(), equalTo(sessionItem.getSessionId()));
        assertThat(item.getAuthorizationCode(), equalTo(sessionItem.getAuthorizationCode()));
    }

    @Test
    void shouldGetAddressSessionItemByTokenIndexSuccessfully() {
        AccessToken accessToken = new BearerAccessToken();
        String serialisedAccessToken = accessToken.toAuthorizationHeader();
        SessionItem item = new SessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAccessToken(serialisedAccessToken);
        List<SessionItem> items = List.of(item);

        when(mockListUtil.getOneItemOrThrowError(items)).thenReturn(item);
        when(mockDataStore.getItemByIndex(SessionItem.ACCESS_TOKEN_INDEX, serialisedAccessToken))
                .thenReturn(items);

        SessionItem sessionItem = sessionService.getSessionByAccessToken(accessToken);
        assertThat(item.getSessionId(), equalTo(sessionItem.getSessionId()));
        assertThat(item.getAccessToken(), equalTo(sessionItem.getAccessToken()));
    }

    @Test
    void shouldThrowExceptionWhenSessionExpired() {
        SessionItem expiredSessionItem = new SessionItem();
        expiredSessionItem.setExpiryDate(fixedInstant.minus(1, ChronoUnit.HOURS).getEpochSecond());
        when(mockDataStore.getItem(SESSION_ID)).thenReturn(expiredSessionItem);

        assertThrows(
                SessionExpiredException.class, () -> sessionService.validateSessionId(SESSION_ID));
    }

    @Test
    void saveAddressesThrowsSessionNotFound() {
        when(mockDataStore.getItem(SESSION_ID)).thenReturn(null);
        assertThrows(
                SessionNotFoundException.class, () -> sessionService.validateSessionId(SESSION_ID));
    }

    @Test
    void shouldUpdateSession() {
        SessionItem sessionItem = new SessionItem();
        sessionService.updateSession(sessionItem);

        verify(mockDataStore).update(sessionItem);
    }

    @Test
    void shouldGetSessionItemBySessionId() {
        sessionService.getSession(SESSION_ID);

        verify(mockDataStore).getItem(SESSION_ID);
    }
}
