package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import uk.gov.di.ipv.cri.address.library.exception.AddressNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.retry.RetryConfig;
import uk.gov.di.ipv.cri.common.library.util.retry.RetryManager;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.di.ipv.cri.address.library.util.CountryCode.isCountryCodeAbsentForAny;
import static uk.gov.di.ipv.cri.address.library.util.CountryCode.isGreatBritain;

public class AddressService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String ADDRESS_TABLE_NAME =
            Optional.ofNullable(System.getenv("ADDRESS_TABLE_NAME"))
                    .orElse("address-address-cri-api-v1");
    private static final String ERROR_SINGLE_ADDRESS_NOT_CURRENT =
            "setAddressValidity found a single address but is not a CURRENT address.";
    private static final String ERROR_TOO_MANY_ADDRESSES =
            "setAddressValidity given too many Addresses to process.";
    private static final String ERROR_COULD_NOT_DETERMINE_CURRENT_ADDRESS =
            "setAddressValidity found two addresses but could not determine which address is a CURRENT addressType.";
    private static final String ERROR_ADDRESS_LINKING_NOT_NEEDED =
            "setAddressValidity found PREVIOUS address but validUntil was already set - automatic date linking failed.";
    private static final String ERROR_ADDRESS_DATE_IS_INVALID =
            "setAddressValidity found address where validFrom and validUntil are Equal.";
    private static final String ERROR_COUNTRY_CODE_NOT_PRESENT =
            "Country code not present for address";
    private static final String ERROR_ADDRESS_ITEM_NOT_PRESENT = "Address Item not found";
    private static final String MANUAL_ADDRESS_METRIC = "manual-address-entry";
    private static final String PRE_POPULATED_ADDRESS_METRIC = "pre-populated-address-entry";
    private final DataStore<AddressItem> dataStore;
    private final ObjectMapper objectMapper;

    private ObjectReader addressReader;

    @ExcludeFromGeneratedCoverageReport
    public AddressService(
            ObjectMapper objectMapper, DynamoDbEnhancedClient dynamoDbEnhancedClient) {

        this(
                new DataStore<>(ADDRESS_TABLE_NAME, AddressItem.class, dynamoDbEnhancedClient),
                objectMapper);
    }

    public AddressService(DataStore<AddressItem> dataStore, ObjectMapper objectMapper) {
        this.dataStore = dataStore;
        this.objectMapper = objectMapper;
    }

    public List<CanonicalAddress> parseAddresses(String addressBody)
            throws AddressProcessingException {
        List<CanonicalAddress> addresses;
        try {
            addresses = getAddressReader().readValue(addressBody);
        } catch (JsonProcessingException e) {
            throw new AddressProcessingException("could not parse addresses..." + e.getMessage());
        }

        if (isCountryCodeAbsentForAny(addresses)) {
            throw new AddressProcessingException(ERROR_COUNTRY_CODE_NOT_PRESENT);
        }
        return addresses;
    }

    public AddressItem saveAddresses(
            UUID sessionId, List<CanonicalAddress> addresses, long ttlExpiryEpoch) {
        AddressItem addressItem = new AddressItem();
        addressItem.setSessionId(sessionId);
        addressItem.setExpiryDate(ttlExpiryEpoch);
        addressItem.setAddresses(
                Optional.ofNullable(addresses).orElse(Collections.emptyList()).stream()
                        .filter(Objects::nonNull)
                        .map(this::normalizePostcodeUkAddresses)
                        .toList());
        dataStore.create(addressItem);

        LOGGER.info(
                "Saved address with TTL: {} and length: {}",
                ttlExpiryEpoch,
                addressItem.getAddresses().size());

        return addressItem;
    }

    private CanonicalAddress normalizePostcodeUkAddresses(CanonicalAddress address) {
        if (isGreatBritain(address.getAddressCountry())) {
            address.setPostalCode(address.getPostalCode().replace(" ", "").toUpperCase());
        }
        return address;
    }

    public AddressItem getAddressItemWithRetries(SessionItem sessionItem) {
        RetryConfig retryConfig = getRetryConfig(500, 3, true);
        return RetryManager.execute(retryConfig, () -> this.getAddressItem(sessionItem));
    }

    public AddressItem getAddressItem(SessionItem sessionItem) {
        try {
            AddressItem addressItem = dataStore.getItem(sessionItem.getSessionId().toString());
            if (addressItem == null) {
                LOGGER.error(
                        "{} for gov uk journey id: {}",
                        ERROR_ADDRESS_ITEM_NOT_PRESENT,
                        sessionItem.getClientSessionId());
                throw new AddressNotFoundException(ERROR_ADDRESS_ITEM_NOT_PRESENT);
            }
            return addressItem;
        } catch (Exception e) {
            LOGGER.error(
                    "Unexpected datastore error for gov uk journey id: {}",
                    sessionItem.getClientSessionId());
            throw new AddressNotFoundException(ERROR_ADDRESS_ITEM_NOT_PRESENT, e);
        }
    }

    // See https://govukverify.atlassian.net/wiki/spaces/PYI/pages/3178004485/Decision+Log
    public void setAddressValidity(List<CanonicalAddress> addresses)
            throws AddressProcessingException {

        switch (addresses.size()) {
            case 0:
                LOGGER.warn("No Addresses to Process.");
                return;
            case 1:
                if (isNotCurrentAddress(addresses.get(0))) {
                    throw new AddressProcessingException(ERROR_SINGLE_ADDRESS_NOT_CURRENT);
                }
                LOGGER.info("Found a Single CURRENT Address.");
                return;
            case 2:
                processAddresses(addresses);
                return;
            default:
                // We cannot link multiple PREVIOUS address dates as they are null.
                // Only the CURRENT address has date information.
                throw new AddressProcessingException(ERROR_TOO_MANY_ADDRESSES);
        }
    }

    public boolean isCurrentAddress(CanonicalAddress canonicalAddress) {

        // Due to PREVIOUS addresses coming from AddressFront without a date set for validUntil we
        // cannot use common-lib to evaluate the type (as they would evaluate as CURRENT addresses)
        // Instead we look for this as a CURRENT address pattern and treat all others as PREVIOUS

        return Objects.nonNull(canonicalAddress.getValidFrom())
                && Objects.isNull(canonicalAddress.getValidUntil());
    }

    public boolean isNotCurrentAddress(CanonicalAddress canonicalAddress) {
        return !isCurrentAddress(canonicalAddress);
    }

    public boolean isInvalidAddress(CanonicalAddress canonicalAddress) {
        return ((Objects.nonNull(canonicalAddress.getValidFrom())
                        && Objects.nonNull(canonicalAddress.getValidUntil()))
                && (canonicalAddress.getValidFrom().isEqual(canonicalAddress.getValidUntil())));
    }

    public void storeAddressEntryTypeMetric(
            EventProbe eventProbe, List<CanonicalAddress> addresses) {
        for (CanonicalAddress address : addresses) {
            if (address.getUprn() != null) {
                eventProbe.counterMetric(PRE_POPULATED_ADDRESS_METRIC);
            } else {
                eventProbe.counterMetric(MANUAL_ADDRESS_METRIC);
            }
        }
    }

    private RetryConfig getRetryConfig(int delayMs, int maxAttempts, boolean exponential) {
        return new RetryConfig.Builder()
                .delayBetweenAttempts(delayMs)
                .maxAttempts(maxAttempts)
                .exponentiallyRetry(exponential)
                .build();
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

    private void processAddresses(List<CanonicalAddress> addresses)
            throws AddressProcessingException {

        CanonicalAddress currentAddress = null;
        CanonicalAddress previousAddress = null;

        CanonicalAddress address0 = addresses.get(0);
        CanonicalAddress address1 = addresses.get(1);

        // Check for a specific invalid case of the dates being equal
        // This would fail against an addressType check in common-lib
        if (isInvalidAddress(address0) || isInvalidAddress(address1)) {
            throw new AddressProcessingException(ERROR_ADDRESS_DATE_IS_INVALID);
        }

        if (isCurrentAddress(address0) && isNotCurrentAddress(address1)) {
            currentAddress = address0;
            previousAddress = address1;
        } else if (isCurrentAddress(address1) && isNotCurrentAddress(address0)) {
            currentAddress = address1;
            previousAddress = address0;
        } else if (isCurrentAddress(address0) && isCurrentAddress(address1)) {

            // This an edge case where there are two CURRENT address.
            // Date linking is not performed.

            LOGGER.info("Found two CURRENT Addresses.");

            return;
        } else {
            throw new AddressProcessingException(ERROR_COULD_NOT_DETERMINE_CURRENT_ADDRESS);
        }

        // When AddressCRI Front is updated to set validUntil the linking needs to be removed
        // else it would trample the dates already set
        if (Objects.nonNull(previousAddress.getValidUntil())) {
            // Not safe to automatically process dates in addresses
            throw new AddressProcessingException(ERROR_ADDRESS_LINKING_NOT_NEEDED);
        }

        LOGGER.info("Found a CURRENT and PREVIOUS Address, linking validFrom with validUntil.");

        // At this point the current address is known
        previousAddress.setValidUntil(currentAddress.getValidFrom());
    }
}
