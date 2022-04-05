package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.nimbusds.oauth2.sdk.ParseException;
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
    @Mock private DataStore<AddressSessionItem> mockAddressSessionDataStore;
    @InjectMocks private CredentialIssuerService addressCredentialIssuerService;

    @Test
    void shouldRetrieveSessionIdWhenInputHasValidAccessToken()
            throws CredentialRequestException, ParseException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        CredentialIssuerService.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        var item = new AddressSessionItem();
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAccessTokenIndex = mock(DynamoDbIndex.class);
        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionDataStore.getItemByGsi(
                        mockAccessTokenIndex, accessToken.toAuthorizationHeader()))
                .thenReturn(Collections.singletonList(item));
        when(mockAddressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX))
                .thenReturn(mockAccessTokenIndex);

        UUID sessionId = addressCredentialIssuerService.getSessionId(event);

        assertThat(sessionId, notNullValue());
        assertThat(item.getSessionId(), equalTo(sessionId));
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenAuthorizationHeaderIsNotSupplied() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();

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
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        CredentialIssuerService.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockAddressSessionDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> addressCredentialIssuerService.getSessionId(event));
        assertThat(exception.getMessage(), containsString("Parameter must have exactly one value"));
    }

    @Test
    void shouldThrowParseExceptionWhenAuthorizationIsNotDoneThruBearerTokenAccess() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        var accessTokenValue = UUID.randomUUID().toString();
        event.withHeaders(
                Map.of(CredentialIssuerService.AUTHORIZATION_HEADER_KEY, accessTokenValue));

        ParseException exception =
                assertThrows(
                        ParseException.class,
                        () -> addressCredentialIssuerService.getSessionId(event));
        assertThat(
                exception.getMessage(), containsString("Invalid HTTP Authorization header value"));
    }
}
