package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialIssuerServiceTest {
    @Mock private DataStore<AddressSessionItem> mockAddressSessionDataStore;
    @Mock private ConfigurationService mockConfigurationService;
    private CredentialIssuerService addressCredentialIssuerService;
    private static Instant fixedInstant;

    @BeforeAll
    static void beforeAll() {
        fixedInstant = Instant.now();
    }

    @BeforeEach
    void setUp() {
        Clock nowClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        AddressSessionService addressSessionService =
                new AddressSessionService(
                        mockAddressSessionDataStore, mockConfigurationService, nowClock);
        addressCredentialIssuerService = new CredentialIssuerService(addressSessionService);
    }

    @Test
    void shouldRetrieveSessionIdWhenInputHasValidAccessToken() throws CredentialRequestException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        var accessTokenValue = UUID.randomUUID().toString();
        event.withQueryStringParameters(
                Map.of(
                        CredentialIssuerService.ACCESS_TOKEN,
                        accessTokenValue,
                        CredentialIssuerService.SUB,
                        "subject"));

        var item = new AddressSessionItem();
        var attVal = AttributeValue.builder().s(accessTokenValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        item.setSessionId(UUID.randomUUID());
        item.setAccessToken(accessTokenValue);
        event.withQueryStringParameters(
                Map.of(CredentialIssuerService.ACCESS_TOKEN, accessTokenValue));

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);
        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(Page.create(List.of(item)));

        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.TOKEN_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);
        when(mockAuthorizationCodeIndex.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);
        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);

        UUID sessionId = addressCredentialIssuerService.getSessionId(event);

        assertThat(sessionId, notNullValue());
        assertThat(item.getSessionId(), equalTo(sessionId));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenSessionIdIsNotFoundUsingTheAccessTokenInTheInput() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        var accessTokenValue = UUID.randomUUID().toString();
        event.withQueryStringParameters(
                Map.of(
                        CredentialIssuerService.ACCESS_TOKEN,
                        accessTokenValue,
                        CredentialIssuerService.SUB,
                        "subject"));

        var attVal = AttributeValue.builder().s(accessTokenValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        event.withQueryStringParameters(
                Map.of(CredentialIssuerService.ACCESS_TOKEN, accessTokenValue));

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);
        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        Stream<Page<AddressSessionItem>> streamedItem =
                Stream.of(Page.create(Collections.emptyList()));

        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.TOKEN_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);
        when(mockAuthorizationCodeIndex.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);

        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> addressCredentialIssuerService.getSessionId(event));
        assertThat(exception.getMessage(), containsString("Parameter must have exactly one value"));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenInTheInputQueryParamHasNoAccessTokenKey() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withQueryStringParameters(Map.of(CredentialIssuerService.SUB, "subject"));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> addressCredentialIssuerService.getSessionId(event));
        assertThat(exception.getMessage(), containsString("Parameter must have exactly one value"));
    }

    @Test
    void shouldRetrieveAddressWhenAValidSessionIdIsSuppliedToGetAddresses() {}
}
