package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.address.library.util.ListUtil;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class SessionService {
    private final ConfigurationService configurationService;
    private final DataStore<SessionItem> dataStore;
    private final ListUtil listUtil;
    private final Clock clock;

    @ExcludeFromGeneratedCoverageReport
    public SessionService() {
        this.configurationService = new ConfigurationService();
        this.dataStore =
                new DataStore<>(
                        configurationService.getSessionTableName(),
                        SessionItem.class,
                        DataStore.getClient());
        this.clock = Clock.systemUTC();
        this.listUtil = new ListUtil();
    }

    public SessionService(
            DataStore<SessionItem> dataStore,
            ConfigurationService configurationService,
            Clock clock,
            ListUtil listUtil) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.clock = clock;
        this.listUtil = listUtil;
    }

    public UUID createAndSaveAddressSession(SessionRequest sessionRequest) {

        SessionItem sessionItem = new SessionItem();
        sessionItem.setExpiryDate(
                clock.instant()
                        .plus(configurationService.getSessionTtl(), ChronoUnit.SECONDS)
                        .getEpochSecond());
        sessionItem.setState(sessionRequest.getState());
        sessionItem.setClientId(sessionRequest.getClientId());
        sessionItem.setRedirectUri(sessionRequest.getRedirectUri());
        sessionItem.setSubject(sessionRequest.getSubject());

        dataStore.create(sessionItem);

        return sessionItem.getSessionId();
    }

    public SessionItem updateSession(SessionItem sessionItem) {
        return dataStore.update(sessionItem);
    }

    public void createAuthorizationCode(SessionItem session) {
        session.setAuthorizationCode(UUID.randomUUID().toString());
        updateSession(session);
    }

    public SessionItem validateSessionId(String sessionId)
            throws SessionNotFoundException, SessionExpiredException {

        SessionItem sessionItem = dataStore.getItem(sessionId);
        if (sessionItem == null) {
            throw new SessionNotFoundException("session not found");
        }

        if (sessionItem.getExpiryDate() < clock.instant().getEpochSecond()) {
            throw new SessionExpiredException("session expired");
        }

        return sessionItem;
    }

    public SessionItem getSession(String sessionId) {
        return dataStore.getItem(sessionId);
    }

    public SessionItem getSessionByAccessToken(AccessToken accessToken) {
        return listUtil.getOneItemOrThrowError(
                dataStore.getItemByIndex(
                        SessionItem.ACCESS_TOKEN_INDEX, accessToken.toAuthorizationHeader()));
    }

    public SessionItem getSessionByAuthorisationCode(String authCode) {
        SessionItem sessionItem =
                listUtil.getOneItemOrThrowError(
                        dataStore.getItemByIndex(SessionItem.AUTHORIZATION_CODE_INDEX, authCode));
        return getSession(sessionItem.getSessionId().toString());
    }
}
