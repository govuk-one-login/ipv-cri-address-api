package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    @Mock private DataStore<AddressSessionItem> mockAddressSessionDataStore;
    @InjectMocks private CredentialIssuerService addressCredentialIssuerService;

    @Test
    void shouldRetrieveSessionIdWhenInputHasValidAccessToken() throws CredentialRequestException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(Map.of(AUTHORIZATION_HEADER_KEY, accessToken.toAuthorizationHeader()));

        var item = new AddressSessionItem();
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAccessTokenIndex = mock(DynamoDbIndex.class);
        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionDataStore.getItemByGsi(
                        mockAccessTokenIndex, accessToken.toAuthorizationHeader()))
                .thenReturn(Collections.singletonList(item));
        when(mockAddressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX))
                .thenReturn(mockAccessTokenIndex);

        AddressSessionItem addressSessionItem =
                addressCredentialIssuerService.getAddressSessionItem(
                        accessToken.toAuthorizationHeader());

        assertThat(item.getAddresses(), notNullValue());
        assertThat(item.getSessionId(), equalTo(addressSessionItem.getSessionId()));
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenAuthorizationHeaderIsNotSupplied() {
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        CredentialRequestException exception =
                assertThrows(
                        CredentialRequestException.class,
                        () ->
                                addressCredentialIssuerService.getAddressSessionItem(
                                        "bad-access-token"));

        assertThat(
                exception.getMessage(),
                containsString(ErrorResponse.MISSING_ADDRESS_SESSION_ITEM.getMessage()));
        assertThat(
                exception.getCause().getMessage(),
                containsString("Parameter must have exactly one value"));
    }

    @Test
    void
            shouldThrowCredentialRequestExceptionWhenSessionIdIsNotFoundUsingTheAccessTokenInTheInput() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(Map.of(AUTHORIZATION_HEADER_KEY, accessToken.toAuthorizationHeader()));

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        CredentialRequestException exception =
                assertThrows(
                        CredentialRequestException.class,
                        () ->
                                addressCredentialIssuerService.getAddressSessionItem(
                                        accessToken.toAuthorizationHeader()));

        assertThat(
                exception.getMessage(),
                containsString(ErrorResponse.MISSING_ADDRESS_SESSION_ITEM.getMessage()));
        assertThat(
                exception.getCause().getMessage(),
                containsString("Parameter must have exactly one value"));
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenAuthorizationIsNotDoneThruBearerTokenAccess() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        var accessTokenValue = UUID.randomUUID().toString();
        event.withHeaders(Map.of(AUTHORIZATION_HEADER_KEY, accessTokenValue));

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        CredentialRequestException exception =
                assertThrows(
                        CredentialRequestException.class,
                        () ->
                                addressCredentialIssuerService.getAddressSessionItem(
                                        accessTokenValue));

        assertThat(
                exception.getMessage(),
                containsString(ErrorResponse.MISSING_ADDRESS_SESSION_ITEM.getMessage()));
        assertThat(
                exception.getCause().getMessage(),
                containsString("Parameter must have exactly one value"));
    }
}
