package uk.gov.di.ipv.cri.address.library.service;

import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

public class AddressSessionService {

    private final DataStore<AddressSessionItem> dataStore;
    private final ConfigurationService configurationService;
    private final Clock clock;

    @ExcludeFromGeneratedCoverageReport
    public AddressSessionService() {
        this.configurationService = new ConfigurationService();
        this.dataStore =
                new DataStore<>(
                        this.configurationService.getAddressSessionTableName(),
                        AddressSessionItem.class,
                        DataStore.getClient());
        clock = Clock.systemUTC();
    }

    public AddressSessionService(
            DataStore<AddressSessionItem> dataStore,
            ConfigurationService configurationService,
            Clock clock) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.clock = clock;
    }

    public String createAndSaveAddressSession() {

        AddressSessionItem addressSessionItem = new AddressSessionItem();
        addressSessionItem.setExpiryDate(
                clock.instant()
                        .plus(configurationService.getAddressSessionTtl(), ChronoUnit.SECONDS)
                        .getEpochSecond());
        dataStore.create(addressSessionItem);
        return addressSessionItem.getSessionId();
    }
}
