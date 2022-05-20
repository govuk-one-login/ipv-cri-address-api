package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AddressService {
    private final ConfigurationService configurationService;
    private final DataStore<AddressItem> dataStore;
    private final ObjectMapper objectMapper;
    private ObjectReader addressReader;

    @ExcludeFromGeneratedCoverageReport
    public AddressService() {
        this.configurationService = new ConfigurationService();
        this.dataStore =
                new DataStore<>(
                        configurationService.getAddressTableName(),
                        AddressItem.class,
                        DataStore.getClient());
        this.objectMapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule());
    }

    public AddressService(
            DataStore<AddressItem> dataStore,
            ConfigurationService configurationService,
            ObjectMapper objectMapper) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
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

    public AddressItem saveAddresses(UUID sessionId, List<CanonicalAddress> addresses) {
        AddressItem addressItem = new AddressItem();

        addressItem.setSessionId(sessionId);
        addressItem.setAddresses(addresses);
        dataStore.create(addressItem);

        return addressItem;
    }

    public AddressItem getAddressItem(UUID sessionId) {
        return dataStore.getItem(String.valueOf(sessionId));
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
