package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.AddressProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.address.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressSessionServiceTest {

    private static Instant fixedInstant;
    private AddressSessionService addressSessionService;

    @Mock private DataStore<AddressSessionItem> mockDataStore;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private ObjectMapper mockObjectMapper;
    @Captor private ArgumentCaptor<AddressSessionItem> mockAddressSessionItem;

    @BeforeAll
    static void beforeAll() {
        fixedInstant = Instant.now();
    }

    @BeforeEach
    void setUp() {
        Clock nowClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        addressSessionService =
                new AddressSessionService(
                        mockDataStore, mockConfigurationService, nowClock, mockObjectMapper);
    }

    @Test
    void shouldCallCreateOnAddressSessionDataStore() {
        when(mockConfigurationService.getAddressSessionTtl()).thenReturn(1L);
        SessionRequest sessionRequest = mock(SessionRequest.class);

        when(sessionRequest.getClientId()).thenReturn("a client id");
        when(sessionRequest.getState()).thenReturn("state");
        when(sessionRequest.getRedirectUri())
                .thenReturn(URI.create("https://www.example.com/callback"));
        when(sessionRequest.getSubject()).thenReturn("a subject");

        addressSessionService.createAndSaveAddressSession(sessionRequest);
        verify(mockDataStore).create(mockAddressSessionItem.capture());
        AddressSessionItem capturedValue = mockAddressSessionItem.getValue();
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
        AddressSessionItem item = new AddressSessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAuthorizationCode(authCodeValue);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockDataStore.getItemByGsi(mockAuthorizationCodeIndex, authCodeValue))
                .thenReturn(List.of(item));
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        AddressSessionItem addressSessionItem =
                addressSessionService.getItemByGSIIndex(
                        authCodeValue, AddressSessionItem.AUTHORIZATION_CODE_INDEX);
        assertThat(item.getSessionId(), equalTo(addressSessionItem.getSessionId()));
        assertThat(item.getAuthorizationCode(), equalTo(addressSessionItem.getAuthorizationCode()));
    }

    @Test
    void shouldGetAddressSessionItemByTokenIndexSuccessfully() {
        String accessTokenValue = UUID.randomUUID().toString();
        AddressSessionItem item = new AddressSessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAccessToken(accessTokenValue);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockTokenIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockDataStore.getItemByGsi(mockTokenIndex, accessTokenValue))
                .thenReturn(List.of(item));
        when(mockAddressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX))
                .thenReturn(mockTokenIndex);

        AddressSessionItem addressSessionItem =
                addressSessionService.getItemByGSIIndex(
                        accessTokenValue, AddressSessionItem.ACCESS_TOKEN_INDEX);
        assertThat(item.getSessionId(), equalTo(addressSessionItem.getSessionId()));
        assertThat(item.getAccessToken(), equalTo(addressSessionItem.getAccessToken()));
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

        List<CanonicalAddress> parsedAddresses = addressSessionService.parseAddresses(addresses);

        assertThat(parsedAddresses.size(), equalTo(readValueResult.size()));
        verify(mockObjectMapper).readerForListOf(CanonicalAddress.class);
        verify(mockObjectReader).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        verify(mockObjectReader).readValue(addresses);
    }

    @Test
    void saveAddressesSetsAuthorizationCode()
            throws SessionExpiredException, SessionNotFoundException {
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

        AddressSessionItem addressSessionItem = new AddressSessionItem();
        addressSessionItem.setExpiryDate(
                Date.from(fixedInstant.plus(Duration.ofDays(1))).getTime());

        when(addressSessionService.getSession(anyString())).thenReturn(addressSessionItem);

        addressSessionService.saveAddresses(String.valueOf(UUID.randomUUID()), addresses);
        verify(mockDataStore).update(mockAddressSessionItem.capture());
        assertThat(mockAddressSessionItem.getValue().getAddresses(), equalTo(addresses));
        assertThat(mockAddressSessionItem.getValue().getAuthorizationCode(), notNullValue());
    }

    @Test
    void saveAddressesThrowsSessionExpired() {
        List<CanonicalAddress> addresses = new ArrayList<>();

        AddressSessionItem addressSessionItem = new AddressSessionItem();

        when(addressSessionService.getSession(anyString())).thenReturn(addressSessionItem);

        assertThrows(
                SessionExpiredException.class,
                () ->
                        addressSessionService.saveAddresses(
                                String.valueOf(UUID.randomUUID()), addresses));
    }

    @Test
    void saveAddressesThrowsSessionNotFound() {
        List<CanonicalAddress> addresses = new ArrayList<>();
        assertThrows(
                SessionNotFoundException.class,
                () ->
                        addressSessionService.saveAddresses(
                                String.valueOf(UUID.randomUUID()), addresses));
    }
}
