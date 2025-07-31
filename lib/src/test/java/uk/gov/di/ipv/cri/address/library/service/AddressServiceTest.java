package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.exception.AddressNotFoundException;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.deserializers.PiiRedactingDeserializer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    private static final UUID SESSION_ID = UUID.randomUUID();
    @Mock private DataStore<AddressItem> mockDataStore;

    private AddressService addressService;

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
    private static final long ADDRESS_TTL = 1730197212;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper
                .registerModule(new JavaTimeModule())
                .registerModule(
                        new SimpleModule()
                                .addDeserializer(
                                        CanonicalAddress.class,
                                        new PiiRedactingDeserializer<>(CanonicalAddress.class)));
        this.addressService = new AddressService(mockDataStore, objectMapper);
    }

    @Nested
    @DisplayName("AddressService parses an Address")
    class AddressServiceParseAddresses {
        @Test
        void shouldParseAddresses() throws AddressProcessingException {
            String addresses =
                    "[\n"
                            + "   {\n"
                            + "      \"uprn\": \"72262801\",\n"
                            + "      \"buildingNumber\": \"8\",\n"
                            + "      \"streetName\": \"GRANGE FIELDS WAY\",\n"
                            + "      \"addressLocality\": \"LEEDS\",\n"
                            + "      \"postalCode\": \"LS10 4QL\",\n"
                            + "      \"addressCountry\": \"GB\",\n"
                            + "      \"addressRegion\": \"YORKSHIRE\",\n"
                            + "      \"validFrom\": \"2010-02-26\",\n"
                            + "      \"validUntil\": \"2021-01-16\"\n"
                            + "   },\n"
                            + "   {\n"
                            + "      \"uprn\": \"63094965\",\n"
                            + "      \"buildingNumber\": \"15\",\n"
                            + "      \"dependentLocality\": \"LOFTHOUSE\",\n"
                            + "      \"streetName\": \"RIDINGS LANE\",\n"
                            + "      \"addressLocality\": \"WAKEFIELD\",\n"
                            + "      \"postalCode\": \"WF3 3SE\",\n"
                            + "      \"addressCountry\": \"GB\",\n"
                            + "      \"validFrom\": \"2021-01-16\",\n"
                            + "      \"validUntil\": \"2021-08-02\"\n"
                            + "   },\n"
                            + "   {\n"
                            + "      \"uprn\": \"63042351\",\n"
                            + "      \"buildingNumber\": \"5\",\n"
                            + "      \"streetName\": \"GATEWAYS\",\n"
                            + "      \"addressLocality\": \"WAKEFIELD\",\n"
                            + "      \"postalCode\": \"WF1 2LZ\",\n"
                            + "      \"addressCountry\": \"GB\",\n"
                            + "      \"validFrom\": \"2021-08-02\"\n"
                            + "   }\n"
                            + "]";

            List<CanonicalAddress> parsedAddresses = addressService.parseAddresses(addresses);

            assertThat(parsedAddresses.size(), equalTo(3));
            assertEquals("YORKSHIRE", parsedAddresses.get(0).getAddressRegion());
        }

        @Test
        void shouldNotRevealPIIAddressDetailWhenAnInvalidDateIsSupplied() {
            String addresses =
                    "[\n"
                            + "   {\n"
                            + "      \"uprn\": \"72262801\",\n"
                            + "      \"buildingNumber\": \"8\",\n"
                            + "      \"streetName\": \"GRANGE FIELDS WAY\",\n"
                            + "      \"addressLocality\": \"LEEDS\",\n"
                            + "      \"postalCode\": \"LS10 4QL\",\n"
                            + "      \"addressCountry\": \"GB\",\n"
                            + "      \"addressRegion\": \"YORKSHIRE\",\n"
                            + "      \"validFrom\": \"2010-00-00\",\n"
                            + "      \"validUntil\": \"2021-01-16\"\n"
                            + "   },\n"
                            + "]";

            AddressProcessingException addressProcessingException =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.parseAddresses(addresses));

            assertThat(
                    addressProcessingException.getMessage(),
                    containsString(
                            String.format(
                                    "could not parse addresses...Error while deserializing object. Some PII fields were redacted. {\"uprn\":\"%s\",\"buildingNumber\":\"*\",\"streetName\":\"%s\",\"addressLocality\":\"%s\",\"postalCode\":\"%s\",\"addressCountry\":\"%s\",\"addressRegion\":\"%s\",\"validFrom\":\"**********\",\"validUntil\":\"**********\"}",
                                    "*".repeat("72262801".length()),
                                    "*".repeat("GRANGE FIELDS WAY".length()),
                                    "*".repeat("LEEDS".length()),
                                    "*".repeat("LS10 4QL".length()),
                                    "*".repeat("GB".length()),
                                    "*".repeat("YORKSHIRE".length()),
                                    "*".repeat("2010-00-00".length()),
                                    "*".repeat("2021-01-16".length()))));
        }

        @Test
        void shouldThrowExceptionWhenNoCountryCodePresentInAddresses() {
            String addresses =
                    "[\n"
                            + "   {\n"
                            + "      \"uprn\": \"72262801\",\n"
                            + "      \"buildingNumber\": \"8\",\n"
                            + "      \"streetName\": \"GRANGE FIELDS WAY\",\n"
                            + "      \"addressLocality\": \"LEEDS\",\n"
                            + "      \"postalCode\": \"LS10 4QL\",\n"
                            + "      \"addressRegion\": \"YORKSHIRE\",\n"
                            + "      \"validFrom\": \"2010-02-26\",\n"
                            + "      \"validUntil\": \"2021-01-16\"\n"
                            + "   },\n"
                            + "   {\n"
                            + "      \"uprn\": \"63094965\",\n"
                            + "      \"buildingNumber\": \"15\",\n"
                            + "      \"dependentLocality\": \"LOFTHOUSE\",\n"
                            + "      \"streetName\": \"RIDINGS LANE\",\n"
                            + "      \"addressLocality\": \"WAKEFIELD\",\n"
                            + "      \"postalCode\": \"WF3 3SE\",\n"
                            + "      \"validFrom\": \"2021-01-16\",\n"
                            + "      \"validUntil\": \"2021-08-02\"\n"
                            + "   },\n"
                            + "   {\n"
                            + "      \"uprn\": \"63042351\",\n"
                            + "      \"buildingNumber\": \"5\",\n"
                            + "      \"streetName\": \"GATEWAYS\",\n"
                            + "      \"addressLocality\": \"WAKEFIELD\",\n"
                            + "      \"postalCode\": \"WF1 2LZ\",\n"
                            + "      \"validFrom\": \"2021-08-02\"\n"
                            + "   }\n"
                            + "]";

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.parseAddresses(addresses));
            assertEquals(ERROR_COUNTRY_CODE_NOT_PRESENT, exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenNoCountryCodePresentInAnAddress() {
            String addresses =
                    "[\n"
                            + "   {\n"
                            + "      \"uprn\": \"72262801\",\n"
                            + "      \"buildingNumber\": \"8\",\n"
                            + "      \"streetName\": \"GRANGE FIELDS WAY\",\n"
                            + "      \"addressLocality\": \"LEEDS\",\n"
                            + "      \"postalCode\": \"LS10 4QL\",\n"
                            + "      \"addressCountry\": \"GB\",\n"
                            + "      \"addressRegion\": \"YORKSHIRE\",\n"
                            + "      \"validFrom\": \"2010-02-26\",\n"
                            + "      \"validUntil\": \"2021-01-16\"\n"
                            + "   },\n"
                            + "   {\n"
                            + "      \"uprn\": \"63094965\",\n"
                            + "      \"buildingNumber\": \"15\",\n"
                            + "      \"dependentLocality\": \"LOFTHOUSE\",\n"
                            + "      \"streetName\": \"RIDINGS LANE\",\n"
                            + "      \"addressLocality\": \"WAKEFIELD\",\n"
                            + "      \"postalCode\": \"WF3 3SE\",\n"
                            + "      \"addressCountry\": \"GB\",\n"
                            + "      \"validFrom\": \"2021-01-16\",\n"
                            + "      \"validUntil\": \"2021-08-02\"\n"
                            + "   },\n"
                            + "   {\n"
                            + "      \"uprn\": \"63042351\",\n"
                            + "      \"buildingNumber\": \"5\",\n"
                            + "      \"streetName\": \"GATEWAYS\",\n"
                            + "      \"addressLocality\": \"WAKEFIELD\",\n"
                            + "      \"postalCode\": \"WF1 2LZ\",\n"
                            + "      \"validFrom\": \"2021-08-02\"\n"
                            + "   }\n"
                            + "]";

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.parseAddresses(addresses));
            assertEquals(ERROR_COUNTRY_CODE_NOT_PRESENT, exception.getMessage());
        }

        @Test
        void shouldReturnEmptyListWhenBodyIsAnEmptyArray() throws AddressProcessingException {
            List<CanonicalAddress> parsedAddresses = addressService.parseAddresses("[]");
            assertTrue(parsedAddresses.isEmpty());
        }
    }

    @Nested
    @DisplayName("AddressService saves an Address")
    class AddressServiceSaveAddresses {
        @Test
        void shouldPersistAddresses() {
            List<CanonicalAddress> addresses = new ArrayList<>();
            CanonicalAddress address1 = new CanonicalAddress();
            address1.setUprn(Long.valueOf("72262801"));
            address1.setBuildingNumber("8");
            address1.setStreetName("GRANGE FIELDS WAY");
            address1.setAddressLocality("LEEDS");
            address1.setPostalCode("LS10 4QL");
            address1.setAddressCountry("GB");
            address1.setAddressRegion("YORKSHIRE");
            address1.setValidFrom(LocalDate.of(2010, 2, 26));
            address1.setValidUntil(LocalDate.of(2021, 1, 16));

            CanonicalAddress address2 = new CanonicalAddress();
            address2.setUprn(Long.valueOf("63094965"));
            address2.setBuildingNumber("15");
            address2.setStreetName("RIDINGS LANE");
            address2.setDependentAddressLocality("LOFTHOUSE");
            address2.setAddressLocality("WAKEFIELD");
            address2.setPostalCode("WF3 3SE");
            address2.setAddressCountry("GB");
            address2.setValidFrom(LocalDate.of(2021, 1, 16));
            address2.setValidUntil(LocalDate.of(2021, 8, 2));

            CanonicalAddress address3 = new CanonicalAddress();
            address3.setUprn(Long.valueOf("63042351"));
            address3.setBuildingNumber("5");
            address3.setStreetName("GATEWAYS");
            address3.setAddressLocality("WAKEFIELD");
            address3.setPostalCode("WF1 2LZ");
            address3.setAddressCountry("GB");
            address3.setValidFrom(LocalDate.of(2021, 8, 2));

            addresses.add(address1);
            addresses.add(address2);
            addresses.add(address3);

            addressService.saveAddresses(SESSION_ID, addresses, ADDRESS_TTL);
            ArgumentCaptor<AddressItem> addressItemArgumentCaptor =
                    ArgumentCaptor.forClass(AddressItem.class);
            verify(mockDataStore).create(addressItemArgumentCaptor.capture());
            MatcherAssert.assertThat(
                    addressItemArgumentCaptor.getValue().getAddresses(), equalTo(addresses));
            MatcherAssert.assertThat(
                    addressItemArgumentCaptor.getValue().getSessionId(), equalTo(SESSION_ID));
            MatcherAssert.assertThat(
                    addressItemArgumentCaptor.getValue().getExpiryDate(), equalTo(ADDRESS_TTL));
        }

        @Test
        void shouldPersistAnEmptyListOfAddressesWhenNoListOfCanonicalAddressesIsSupplied() {
            addressService.saveAddresses(SESSION_ID, null, ADDRESS_TTL);
            ArgumentCaptor<AddressItem> addressItemArgumentCaptor =
                    ArgumentCaptor.forClass(AddressItem.class);
            verify(mockDataStore).create(addressItemArgumentCaptor.capture());
            MatcherAssert.assertThat(
                    addressItemArgumentCaptor.getValue().getAddresses(),
                    equalTo(new ArrayList<>()));
            MatcherAssert.assertThat(
                    addressItemArgumentCaptor.getValue().getSessionId(), equalTo(SESSION_ID));
            MatcherAssert.assertThat(
                    addressItemArgumentCaptor.getValue().getExpiryDate(), equalTo(ADDRESS_TTL));
        }
    }

    @Nested
    @DisplayName("AddressService sets Address Validity")
    class AddressServiceSetAddressValidity {
        @Test
        void shouldSucceedWithNoAddresses() {
            List<CanonicalAddress> canonicalAddresses = new ArrayList<>();
            assertDoesNotThrow(() -> addressService.setAddressValidity(canonicalAddresses));
        }

        @Test
        void shouldEvaluateToPreviousWithNULLNULL() {
            // The reason for the linker existing is that a second address, intended as
            // a PREVIOUS address has ValidFrom and ValidUntil not set - null,null. This however
            // matches the pattern for a CURRENT address with unknown start date.
            // In this specific case, we evaluate null, null as the intended PREVIOUS address.

            CanonicalAddress currentAddress = new CanonicalAddress();
            currentAddress.setValidFrom(null);
            currentAddress.setValidUntil(null);

            List<CanonicalAddress> canonicalAddresses = List.of(currentAddress);

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.setAddressValidity(canonicalAddresses));
            assertEquals(ERROR_SINGLE_ADDRESS_NOT_CURRENT, exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenAboutToTrampleAlreadySetDates() {
            // When AddressCRIFront is setting ValidUntil in previous addresses
            // The linker can be removed entirely.

            final LocalDate TEST_DATE_0 = LocalDate.of(1999, 12, 31);
            final LocalDate TEST_DATE_1 = LocalDate.of(1999, 11, 30);

            CanonicalAddress currentAddress = new CanonicalAddress();
            currentAddress.setValidFrom(TEST_DATE_0);
            currentAddress.setValidUntil(null);

            CanonicalAddress previousAddress = new CanonicalAddress();
            previousAddress.setValidFrom(null);
            previousAddress.setValidUntil(TEST_DATE_1);

            List<CanonicalAddress> canonicalAddresses = List.of(currentAddress, previousAddress);

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.setAddressValidity(canonicalAddresses));

            assertEquals(ERROR_ADDRESS_LINKING_NOT_NEEDED, exception.getMessage());
        }

        @Test
        void shouldSucceedWithSingleCurrentAddress() {

            LocalDate date = LocalDate.of(2013, 8, 9);

            CanonicalAddress currentAddress = new CanonicalAddress();
            currentAddress.setValidFrom(date);

            List<CanonicalAddress> canonicalAddresses = List.of(currentAddress);

            assertDoesNotThrow(() -> addressService.setAddressValidity(canonicalAddresses));
        }

        @Test
        void shouldThrowExceptionWhenGivenSingleInvalidAddress() {

            CanonicalAddress currentAddress = new CanonicalAddress();
            currentAddress.setValidFrom(null);
            currentAddress.setValidUntil(null);

            List<CanonicalAddress> canonicalAddresses = List.of(currentAddress);

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.setAddressValidity(canonicalAddresses));

            assertEquals(ERROR_SINGLE_ADDRESS_NOT_CURRENT, exception.getMessage());
        }

        @Test
        void shouldSucceedWithTwoValidAddresses() {

            LocalDate date = LocalDate.of(2013, 8, 9);

            CanonicalAddress currentAddress = new CanonicalAddress();
            currentAddress.setValidFrom(date);

            CanonicalAddress previousAddress = new CanonicalAddress();
            previousAddress.setValidFrom(null);
            previousAddress.setValidUntil(null);

            assertNull(previousAddress.getValidUntil());

            List<CanonicalAddress> canonicalAddresses = List.of(currentAddress, previousAddress);

            assertDoesNotThrow(() -> addressService.setAddressValidity(canonicalAddresses));

            // Linking performed
            assertTrue(currentAddress.getValidFrom().isEqual(previousAddress.getValidUntil()));
        }

        @Test
        void shouldSucceedWithTwoCurrentAddresses() {

            LocalDate date = LocalDate.of(2013, 8, 9);

            CanonicalAddress currentAddress0 = new CanonicalAddress();
            currentAddress0.setValidFrom(date);

            CanonicalAddress currentAddress1 = new CanonicalAddress();
            currentAddress1.setValidFrom(date.plusYears(1));
            currentAddress1.setValidUntil(null);

            List<CanonicalAddress> canonicalAddresses = List.of(currentAddress0, currentAddress1);

            assertDoesNotThrow(() -> addressService.setAddressValidity(canonicalAddresses));
        }

        @Test
        void shouldThrowExceptionWhenGivenTwoInvalidAddresses() {

            CanonicalAddress address0 = new CanonicalAddress();
            address0.setValidFrom(null);
            address0.setValidUntil(null);

            CanonicalAddress address1 = new CanonicalAddress();
            address1.setValidFrom(null);
            address1.setValidUntil(null);

            List<CanonicalAddress> canonicalAddresses = List.of(address0, address1);

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.setAddressValidity(canonicalAddresses));

            assertEquals(ERROR_COULD_NOT_DETERMINE_CURRENT_ADDRESS, exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenGivenTwoPreviousAddresses() {

            LocalDate date = LocalDate.of(2013, 8, 9);

            CanonicalAddress previousAddress0 = new CanonicalAddress();
            previousAddress0.setValidFrom(date);
            previousAddress0.setValidUntil(date.plusYears(1));

            CanonicalAddress previousAddress1 = new CanonicalAddress();
            previousAddress1.setValidFrom(date.plusYears(2));
            previousAddress1.setValidUntil(date.plusYears(3));

            List<CanonicalAddress> canonicalAddresses = List.of(previousAddress0, previousAddress1);

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.setAddressValidity(canonicalAddresses));

            assertEquals(ERROR_COULD_NOT_DETERMINE_CURRENT_ADDRESS, exception.getMessage());
        }

        @Test
        void shouldSucceedWithTwoValidAddressesInReverseOrder() {

            LocalDate date = LocalDate.of(2013, 8, 9);

            CanonicalAddress currentAddress = new CanonicalAddress();
            currentAddress.setValidFrom(date);

            CanonicalAddress previousAddress = new CanonicalAddress();
            previousAddress.setValidFrom(null);
            previousAddress.setValidUntil(null);

            assertNull(previousAddress.getValidUntil());

            // Order Reversed
            List<CanonicalAddress> canonicalAddresses = List.of(previousAddress, currentAddress);

            assertDoesNotThrow(() -> addressService.setAddressValidity(canonicalAddresses));

            // Linking performed
            assertTrue(currentAddress.getValidFrom().isEqual(previousAddress.getValidUntil()));
        }

        @Test
        void shouldThrowExceptionGivenInvalidFirstAddressDates() {

            LocalDate date = LocalDate.of(2013, 8, 9);

            CanonicalAddress address0 = new CanonicalAddress();
            address0.setValidFrom(date);
            address0.setValidUntil(date);

            CanonicalAddress address1 = new CanonicalAddress();
            address1.setValidFrom(date);
            address1.setValidUntil(null);

            List<CanonicalAddress> canonicalAddresses = List.of(address0, address1);

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.setAddressValidity(canonicalAddresses));

            assertEquals(ERROR_ADDRESS_DATE_IS_INVALID, exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenGivenInvalidSecondAddressDates() {

            LocalDate date = LocalDate.of(2013, 8, 9);

            CanonicalAddress address0 = new CanonicalAddress();
            address0.setValidFrom(date);
            address0.setValidUntil(null);

            CanonicalAddress address1 = new CanonicalAddress();
            address1.setValidFrom(date);
            address1.setValidUntil(date);

            // Reversed
            List<CanonicalAddress> canonicalAddresses = List.of(address1, address0);

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.setAddressValidity(canonicalAddresses));

            assertEquals(ERROR_ADDRESS_DATE_IS_INVALID, exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenGivenThreeAddresses() {

            LocalDate date = LocalDate.of(2013, 8, 9);

            CanonicalAddress currentAddress = new CanonicalAddress();
            currentAddress.setValidFrom(date);

            CanonicalAddress previousAddress1 = new CanonicalAddress();
            previousAddress1.setValidFrom(null);
            previousAddress1.setValidUntil(null);

            CanonicalAddress previousAddress2 = new CanonicalAddress();
            previousAddress2.setValidFrom(null);
            previousAddress2.setValidUntil(null);

            List<CanonicalAddress> canonicalAddresses =
                    List.of(currentAddress, previousAddress1, previousAddress2);

            AddressProcessingException exception =
                    assertThrows(
                            AddressProcessingException.class,
                            () -> addressService.setAddressValidity(canonicalAddresses));

            assertEquals(ERROR_TOO_MANY_ADDRESSES, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Address Service is current Address")
    class AddressServiceIsCurrentAddress {
        @Test
        void shouldReturnTrueWithExpectedCurrentFormatDates() {
            CanonicalAddress address = new CanonicalAddress();
            address.setValidFrom(LocalDate.now().minusYears(1));
            address.setValidUntil(null);

            assertTrue(addressService.isCurrentAddress(address));
        }

        @Test
        void shouldReturnFalseWithExpectedPreviousFormatDates() {
            CanonicalAddress address = new CanonicalAddress();
            address.setValidFrom(null);
            address.setValidUntil(LocalDate.now().minusYears(1));

            assertFalse(addressService.isCurrentAddress(address));
        }

        @Test
        void shouldReturnFalseWithExpectedInvalidFormatDates() {
            CanonicalAddress address = new CanonicalAddress();
            address.setValidFrom(LocalDate.now().minusYears(1));
            address.setValidUntil(LocalDate.now().minusYears(1));

            assertFalse(addressService.isCurrentAddress(address));
        }
    }

    @Nested
    @DisplayName("Address Service is supplied an invalid Address")
    class AddressServiceIsSuppliedInvalidAddress {
        @Test
        void shouldReturnTrueWithInvalidFormatDates() {
            CanonicalAddress address = new CanonicalAddress();
            address.setValidFrom(LocalDate.now());
            address.setValidUntil(LocalDate.now());

            assertTrue(addressService.isInvalidAddress(address));
        }

        @Test
        void shouldReturnFalseWithExpectedCurrentFormatDates() {
            CanonicalAddress address = new CanonicalAddress();
            address.setValidFrom(LocalDate.now().minusYears(1));
            address.setValidUntil(null);

            assertFalse(addressService.isInvalidAddress(address));
        }

        @Test
        void shouldReturnFalseWhenGivenExpectedPreviousFormatDates() {
            CanonicalAddress address = new CanonicalAddress();
            address.setValidFrom(null);
            address.setValidUntil(LocalDate.now().minusYears(1));

            assertFalse(addressService.isInvalidAddress(address));
        }
    }

    @Test
    void shouldGetAddressItemUsingSessionItem() {
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSessionId(SESSION_ID);
        addressService.getAddressItem(sessionItem);

        verify(mockDataStore).getItem(sessionItem.getSessionId().toString());
    }

    @Test
    void throwsNullPointerExceptionWhenAddressIsNotFoundWithSessionItem() {
        assertThrows(
                NullPointerException.class,
                () -> addressService.getAddressItem((SessionItem) null));
    }

    @Test
    void getAddressItemWithRetriesAllRetriesFail() {
        when(mockDataStore.getItem(anyString()))
                .thenThrow(new RuntimeException())
                .thenThrow(new RuntimeException())
                .thenThrow(new RuntimeException());

        var sessionItem = new SessionItem();
        AddressNotFoundException ex =
                assertThrows(
                        AddressNotFoundException.class,
                        () -> addressService.getAddressItemWithRetries(sessionItem));

        assertEquals("Address Item not found", ex.getMessage());
        verify(mockDataStore, times(3)).getItem(sessionItem.getSessionId().toString());
    }

    @Test
    void getAddressItemSomeWithRetriesSucceedsAfterRetry() {
        AddressItem addressItem = new AddressItem();
        CanonicalAddress canonicalAddress = new CanonicalAddress();
        canonicalAddress.setAddressCountry("GB");
        addressItem.setAddresses(Collections.singletonList(canonicalAddress));

        when(mockDataStore.getItem(anyString()))
                .thenThrow(new RuntimeException("temporary failure"))
                .thenReturn(addressItem);

        var sessionItem = new SessionItem();
        AddressItem result = addressService.getAddressItemWithRetries(sessionItem);

        assertEquals(addressItem, result);
        verify(mockDataStore, times(2)).getItem(sessionItem.getSessionId().toString());
    }

    @Test
    void throwsAddressIsNotFoundWhenDataSoreGetItemFails() {
        when(mockDataStore.getItem(anyString())).thenThrow(new RuntimeException("some db error"));

        var sessionItem = new SessionItem();
        AddressNotFoundException ex =
                assertThrows(
                        AddressNotFoundException.class,
                        () -> addressService.getAddressItem(sessionItem));

        assertEquals("Address Item not found", ex.getMessage());
        verify(mockDataStore).getItem(sessionItem.getSessionId().toString());
    }

    @Nested
    @DisplayName("Address Service stores entry type metric")
    class AddressServiceStoredEntryMetric {

        private static final String MANUAL_ADDRESS_METRIC = "manual-address-entry";
        private static final String PRE_POPULATED_ADDRESS_METRIC = "pre-populated-address-entry";

        private static final CanonicalAddress manualAddress = new CanonicalAddress();
        private static final CanonicalAddress prepopulatedAddress = new CanonicalAddress();

        @Mock private EventProbe eventProbe;

        @BeforeAll
        static void setupAddress() {
            prepopulatedAddress.setUprn(123L);
        }

        @Test
        void storesManualEntryMetric() {
            addressService.storeAddressEntryTypeMetric(eventProbe, List.of(manualAddress));

            verify(eventProbe, times(1)).counterMetric(MANUAL_ADDRESS_METRIC);
            verify(eventProbe, times(0)).counterMetric(PRE_POPULATED_ADDRESS_METRIC);
        }

        @Test
        void storesPrePopulatedEntryMetric() {
            CanonicalAddress address = new CanonicalAddress();
            address.setUprn(123L);

            addressService.storeAddressEntryTypeMetric(eventProbe, List.of(prepopulatedAddress));

            verify(eventProbe, times(0)).counterMetric(MANUAL_ADDRESS_METRIC);
            verify(eventProbe, times(1)).counterMetric(PRE_POPULATED_ADDRESS_METRIC);
        }

        @Test
        void storesMultipleManualEntryMetrics() {
            addressService.storeAddressEntryTypeMetric(
                    eventProbe, List.of(manualAddress, manualAddress));

            verify(eventProbe, times(2)).counterMetric(MANUAL_ADDRESS_METRIC);
            verify(eventProbe, times(0)).counterMetric(PRE_POPULATED_ADDRESS_METRIC);
        }

        @Test
        void storesMultiplePrePopulatedEntryMetrics() {
            addressService.storeAddressEntryTypeMetric(
                    eventProbe, List.of(prepopulatedAddress, prepopulatedAddress));

            verify(eventProbe, times(0)).counterMetric(MANUAL_ADDRESS_METRIC);
            verify(eventProbe, times(2)).counterMetric(PRE_POPULATED_ADDRESS_METRIC);
        }

        @Test
        void storesBothManualAnaPrepopulatedEntryMetrics() {
            addressService.storeAddressEntryTypeMetric(
                    eventProbe, List.of(prepopulatedAddress, manualAddress));

            verify(eventProbe, times(1)).counterMetric(MANUAL_ADDRESS_METRIC);
            verify(eventProbe, times(1)).counterMetric(PRE_POPULATED_ADDRESS_METRIC);
        }

        @Test
        void storesNoMetricsIfAddressesEmpty() {
            addressService.storeAddressEntryTypeMetric(eventProbe, Collections.emptyList());

            verify(eventProbe, times(0)).counterMetric(MANUAL_ADDRESS_METRIC);
            verify(eventProbe, times(0)).counterMetric(PRE_POPULATED_ADDRESS_METRIC);
        }
    }
}
