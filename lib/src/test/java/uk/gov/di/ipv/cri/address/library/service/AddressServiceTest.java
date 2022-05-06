package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.domain.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    private static final UUID SESSION_ID = UUID.randomUUID();
    @Mock private DataStore<AddressItem> mockDataStore;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private ConfigurationService mockConfigurationService;

    private AddressService addressService;

    @BeforeEach
    void setup() {
        this.addressService =
                new AddressService(mockDataStore, mockConfigurationService, mockObjectMapper);
    }

    @Test
    void shouldParseAddresses() throws AddressProcessingException, JsonProcessingException {
        String addresses =
                "[\n"
                        + "   {\n"
                        + "      \"uprn\": \"72262801\",\n"
                        + "      \"buildingNumber\": \"8\",\n"
                        + "      \"streetName\": \"GRANGE FIELDS WAY\",\n"
                        + "      \"addressLocality\": \"LEEDS\",\n"
                        + "      \"postalCode\": \"LS10 4QL\",\n"
                        + "      \"addressCountry\": \"GBR\",\n"
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
                        + "      \"addressCountry\": \"GBR\",\n"
                        + "      \"validFrom\": \"2021-01-16\",\n"
                        + "      \"validUntil\": \"2021-08-02\"\n"
                        + "   },\n"
                        + "   {\n"
                        + "      \"uprn\": \"63042351\",\n"
                        + "      \"buildingNumber\": \"5\",\n"
                        + "      \"streetName\": \"GATEWAYS\",\n"
                        + "      \"addressLocality\": \"WAKEFIELD\",\n"
                        + "      \"postalCode\": \"WF1 2LZ\",\n"
                        + "      \"addressCountry\": \"GBR\",\n"
                        + "      \"validFrom\": \"2021-08-02\"\n"
                        + "   }\n"
                        + "]";

        List<CanonicalAddress> readValueResult = List.of(new CanonicalAddress());
        ObjectReader mockObjectReader = Mockito.mock(ObjectReader.class);
        when(mockObjectReader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                .thenReturn(mockObjectReader);
        when(mockObjectMapper.readerForListOf(CanonicalAddress.class)).thenReturn(mockObjectReader);
        when(mockObjectReader.readValue(addresses)).thenReturn(readValueResult);

        List<CanonicalAddress> parsedAddresses = addressService.parseAddresses(addresses);

        assertThat(parsedAddresses.size(), equalTo(readValueResult.size()));
        verify(mockObjectMapper).readerForListOf(CanonicalAddress.class);
        verify(mockObjectReader).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        verify(mockObjectReader).readValue(addresses);
    }

    @Test
    void shouldPersistAddresses() {
        List<CanonicalAddress> addresses = new ArrayList<>();
        CanonicalAddress address1 = new CanonicalAddress();
        address1.setUprn(Long.valueOf("72262801"));
        address1.setBuildingNumber("8");
        address1.setStreetName("GRANGE FIELDS WAY");
        address1.setAddressLocality("LEEDS");
        address1.setPostalCode("LS10 4QL");
        address1.setAddressCountry("GBR");
        address1.setValidFrom(Date.from(Instant.parse("2010-02-26T00:00:00.00Z")));
        address1.setValidUntil(Date.from(Instant.parse("2021-01-16T00:00:00.00Z")));

        CanonicalAddress address2 = new CanonicalAddress();
        address2.setUprn(Long.valueOf("63094965"));
        address2.setBuildingNumber("15");
        address2.setStreetName("RIDINGS LANE");
        address2.setDependentAddressLocality("LOFTHOUSE");
        address2.setAddressLocality("WAKEFIELD");
        address2.setPostalCode("WF3 3SE");
        address2.setAddressCountry("GBR");
        address2.setValidFrom(Date.from(Instant.parse("2021-01-16T00:00:00.00Z")));
        address2.setValidUntil(Date.from(Instant.parse("2021-08-02T00:00:00.00Z")));

        CanonicalAddress address3 = new CanonicalAddress();
        address3.setUprn(Long.valueOf("63042351"));
        address3.setBuildingNumber("5");
        address3.setStreetName("GATEWAYS");
        address3.setAddressLocality("WAKEFIELD");
        address3.setPostalCode("WF1 2LZ");
        address3.setAddressCountry("GBR");
        address3.setValidFrom(Date.from(Instant.parse("2021-08-02T00:00:00.00Z")));

        addresses.add(address1);
        addresses.add(address2);
        addresses.add(address3);

        addressService.saveAddresses(SESSION_ID, addresses);
        ArgumentCaptor<AddressItem> addressItemArgumentCaptor =
                ArgumentCaptor.forClass(AddressItem.class);
        verify(mockDataStore).create(addressItemArgumentCaptor.capture());
        MatcherAssert.assertThat(
                addressItemArgumentCaptor.getValue().getAddresses(), equalTo(addresses));
        MatcherAssert.assertThat(
                addressItemArgumentCaptor.getValue().getSessionId(), equalTo(SESSION_ID));
    }

    @Test
    void shouldGetAddresses() {
        addressService.getAddress(SESSION_ID);
        verify(mockDataStore).getItem(String.valueOf(SESSION_ID));
    }
}
