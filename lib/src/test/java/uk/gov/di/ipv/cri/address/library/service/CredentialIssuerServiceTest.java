package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

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
    private CredentialIssuerService addressCredentialIssuerService;

    @BeforeEach
    void setUp() {
        addressCredentialIssuerService = new CredentialIssuerService(mockAddressSessionDataStore);
    }

    @Test
    void shouldRetrieveSessionIdWhenInputHasValidAccessToken() throws CredentialRequestException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        var accessTokenValue = UUID.randomUUID().toString();
        event.withHeaders(Map.of(CredentialIssuerService.AUTHORIZATION, accessTokenValue));
        event.withBody("sub=subject");

        var item = new AddressSessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAccessToken(accessTokenValue);
        event.withHeaders(Map.of(CredentialIssuerService.AUTHORIZATION, accessTokenValue));

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAccessTokenIndex = mock(DynamoDbIndex.class);

        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionDataStore.getItemByGsi(mockAccessTokenIndex, accessTokenValue))
                .thenReturn(Collections.singletonList(item));
        when(mockAddressSessionTable.index(AddressSessionItem.TOKEN_INDEX))
                .thenReturn(mockAccessTokenIndex);

        UUID sessionId = addressCredentialIssuerService.getSessionId(event);

        assertThat(sessionId, notNullValue());
        assertThat(item.getSessionId(), equalTo(sessionId));
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenAuthorizationHeaderIsNotSupplied() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withBody("sub=subject");

        CredentialRequestException exception =
                assertThrows(
                        CredentialRequestException.class,
                        () -> addressCredentialIssuerService.getSessionId(event));

        assertThat(
                exception.getMessage(),
                containsString(ErrorResponse.MISSING_AUTHORIZATION_HEADER.getMessage()));
    }

    @Test
    void shouldThrowIlegalArgumentExceptionWhenSessionIdIsNotFoundUsingTheAccessTokenInTheInput() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        var accessTokenValue = UUID.randomUUID().toString();
        event.withHeaders(Map.of(CredentialIssuerService.AUTHORIZATION, accessTokenValue));
        event.withBody("sub=subject");

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.TOKEN_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

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

        CredentialRequestException exception =
                assertThrows(
                        CredentialRequestException.class,
                        () -> addressCredentialIssuerService.getSessionId(event));
        assertThat(
                exception.getMessage(),
                containsString(ErrorResponse.INVALID_REQUEST_PARAM.getMessage()));
    }
}
