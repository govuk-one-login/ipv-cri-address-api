package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.common.contenttype.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;
import uk.gov.di.ipv.cri.address.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.address.library.service.CredentialIssuerService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueCredentialHandlerTest {
    @Mock private Context context;
    @Mock private CredentialIssuerService mockAddressCredentialIssuerService;
    private IssueCredentialHandler handler;

    @BeforeEach
    void setUp() {
        handler = new IssueCredentialHandler(mockAddressCredentialIssuerService);
    }

    @Test
    void shouldReturnAddressAndAOneValueWhenIssueCredentialRequestIsValid()
            throws CredentialRequestException {
        UUID sessionId = UUID.randomUUID();
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withQueryStringParameters(
                Map.of(
                        CredentialIssuerService.ACCESS_TOKEN, "12345",
                        CredentialIssuerService.SUB, "subject"));
        when(mockAddressCredentialIssuerService.getSessionId(event)).thenReturn(sessionId);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockAddressCredentialIssuerService).getAddresses(sessionId);
        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        System.out.println(response.getBody());
        assertEquals(response.getBody(), "1");
    }

    @Test
    void shouldReturnWithAZeroValueWhenIssueCredentialRequestIsInValid() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        ConfigurationService mockConfigurationService = mock(ConfigurationService.class);
        DataStore<AddressSessionItem> mockAddressDataStore = mock(DataStore.class);
        AddressSessionService spyAddressSessionService =
                Mockito.spy(
                        new AddressSessionService(
                                mockAddressDataStore,
                                mockConfigurationService,
                                Clock.fixed(Instant.now(), ZoneId.systemDefault())));
        var accessTokenValue = UUID.randomUUID().toString();
        var item = new AddressSessionItem();
        var attVal = AttributeValue.builder().s(accessTokenValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        item.setSessionId(UUID.randomUUID());
        item.setAuthorizationCode(accessTokenValue);
        event.withQueryStringParameters(
                Map.of(CredentialIssuerService.ACCESS_TOKEN, accessTokenValue));

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);
        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(Page.create(List.of(item)));
        when(mockAddressDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.TOKEN_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);
        when(mockAuthorizationCodeIndex.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);
        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);
        handler = new IssueCredentialHandler(new CredentialIssuerService(spyAddressSessionService));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals(response.getBody(), "0");
    }
}
