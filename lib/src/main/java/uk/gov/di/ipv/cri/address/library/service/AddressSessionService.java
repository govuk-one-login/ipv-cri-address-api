package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.helpers.ListUtil;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AddressSessionService {
    private final ConfigurationService configurationService;
    private final DataStore<AddressSessionItem> dataStore;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private ObjectReader addressReader;

    @ExcludeFromGeneratedCoverageReport
    public AddressSessionService() {
        this.configurationService = new ConfigurationService();
        this.dataStore =
                new DataStore<>(
                        configurationService.getAddressSessionTableName(),
                        AddressSessionItem.class,
                        DataStore.getClient());
        this.clock = Clock.systemUTC();
        this.objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
    }

    public AddressSessionService(
            DataStore<AddressSessionItem> dataStore,
            ConfigurationService configurationService,
            Clock clock,
            ObjectMapper objectMapper) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public UUID createAndSaveAddressSession(SessionRequest sessionRequest) {

        AddressSessionItem addressSessionItem = new AddressSessionItem();
        addressSessionItem.setExpiryDate(
                clock.instant()
                        .plus(configurationService.getAddressSessionTtl(), ChronoUnit.SECONDS)
                        .getEpochSecond());
        addressSessionItem.setState(sessionRequest.getState());
        addressSessionItem.setClientId(sessionRequest.getClientId());
        addressSessionItem.setRedirectUri(sessionRequest.getRedirectUri());
        addressSessionItem.setSubject(sessionRequest.getSubject());

        dataStore.create(addressSessionItem);

        return addressSessionItem.getSessionId();
    }

    public List<CanonicalAddress> parseAddresses(String addressBody)
            throws AddressProcessingException {
        List<CanonicalAddress> addresses;
        try {
            addresses = getAddressReader().readValue(addressBody);
        } catch (JsonProcessingException e) {
            throw new AddressProcessingException(
                    "could not parse addresses..." + e.getMessage(), e);
        }

        return addresses;
    }

    public AddressSessionItem getSession(String sessionId) {
        return dataStore.getItem(sessionId);
    }

    public void update(AddressSessionItem addressSessionItem) {
        dataStore.update(addressSessionItem);
    }

    public void validateSessionId(String sessionId)
            throws SessionNotFoundException, SessionExpiredException {

        AddressSessionItem sessionItem = dataStore.getItem(sessionId);
        if (sessionItem == null) {
            throw new SessionNotFoundException("session not found");
        }

        if (sessionItem.getExpiryDate() < clock.instant().getEpochSecond()) {
            throw new SessionExpiredException("session expired");
        }
    }

    public AddressSessionItem saveAddresses(String sessionId, List<CanonicalAddress> addresses)
            throws SessionExpiredException, SessionNotFoundException {
        validateSessionId(sessionId);

        var sessionItem = dataStore.getItem(sessionId);
        if (sessionItem == null) {
            throw new SessionNotFoundException("session not found");
        }

        sessionItem.setAddresses(addresses);
        sessionItem.setAuthorizationCode(UUID.randomUUID().toString());
        dataStore.update(sessionItem);

        return sessionItem;
    }

    public AddressSessionItem getItemByGSIIndex(final String value, String indexName) {
        DynamoDbTable<AddressSessionItem> addressSessionTable = dataStore.getTable();
        DynamoDbIndex<AddressSessionItem> index = addressSessionTable.index(indexName);
        var listHelper = new ListUtil();

        return listHelper.getOneItemOrThrowError(dataStore.getItemByGsi(index, value));
    }

    private ObjectReader getAddressReader() {
        if (Objects.isNull(this.addressReader)) {
            this.addressReader =
                    this.objectMapper
                            .readerForListOf(CanonicalAddress.class)
                            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        return this.addressReader;
    }
}
