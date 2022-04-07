package uk.gov.di.ipv.cri.address.library.service;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenRequestException;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenValidationException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {
    @Mock private DataStore<AddressSessionItem> mockDataStore;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private JWTVerifier jwtVerifier;
    private AccessTokenService accessTokenService;

    @BeforeEach
    void setUp() {
        accessTokenService =
                new AccessTokenService(
                        mockDataStore,
                        Duration.ofHours(1).getSeconds(),
                        mockConfigurationService,
                        jwtVerifier);
    }

    @Test
    void shouldCallCreateTokenRequestSuccessfully() throws AccessTokenValidationException {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "code=%S&redirect_uri=%S&grant_type=authorization_code&client_id=test_client_id",
                        authCodeValue, redirectUri);

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        when(mockAddressSessionItem.getAuthorizationCode()).thenReturn(authCodeValue);
        when(mockAddressSessionItem.getRedirectUri()).thenReturn(URI.create(redirectUri));
        when(mockDataStore.getItemByGsi(mockAuthorizationCodeIndex, authCodeValue))
                .thenReturn(List.of(mockAddressSessionItem));

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);
        AuthorizationCodeGrant authorizationCodeGrant =
                (AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant();
        assertThat(tokenRequest, notNullValue());
        assertThat(
                authorizationCodeGrant.getAuthorizationCode().getValue(), equalTo(authCodeValue));
    }

    @Test
    void shouldThrowInvalidRequestWhenTokenRequestUrlParamsIsInComplete() {
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "redirect_uri=%S&grant_type=authorization_code&client_id=test_client_id",
                        redirectUri);

        AccessTokenRequestException exception =
                assertThrows(
                        AccessTokenRequestException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString(OAuth2Error.INVALID_REQUEST_CODE));
    }

    @Test
    void shouldThrowExceptionWhenAddressSessionItemDoesNotExistForTheRequestedAuthorizationCode() {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "code=%S&redirect_uri=%S&grant_type=authorization_code&client_id=test_client_id",
                        authCodeValue, redirectUri);

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockAddressSessionItem.getAuthorizationCode()).thenReturn("wrong-authorization-code");
        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockDataStore.getItemByGsi(mockAuthorizationCodeIndex, authCodeValue))
                .thenReturn(List.of(mockAddressSessionItem));
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        AccessTokenRequestException exception =
                assertThrows(
                        AccessTokenRequestException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Cannot retrieve Address session item for the given authorization Code"));
        assertThat(exception.getErrorObject(), equalTo(OAuth2Error.INVALID_GRANT));
    }

    @Test
    void shouldThrowExceptionWhenNoMatchingAddressSessionItemForTheRequestedAuthorizationCode() {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "code=%S&redirect_uri=%S&grant_type=authorization_code&client_id=test_client_id",
                        authCodeValue, redirectUri);

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockDataStore.getItemByGsi(mockAuthorizationCodeIndex, authCodeValue))
                .thenReturn(List.of(mockAddressSessionItem));
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        AccessTokenRequestException exception =
                assertThrows(
                        AccessTokenRequestException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Cannot retrieve Address session item for the given authorization Code"));
        assertThat(exception.getErrorObject(), equalTo(OAuth2Error.INVALID_GRANT));
    }

    @Test
    void shouldThrowUnSupportGrantTypeExceptionWhenAuthorizationGrantTypeIsInValid() {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "code=%S&redirect_uri=%S&grant_type=implicit&client_id=test_client_id",
                        authCodeValue, redirectUri);

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockDataStore.getItemByGsi(mockAuthorizationCodeIndex, authCodeValue))
                .thenReturn(List.of(mockAddressSessionItem));
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        AccessTokenRequestException exception =
                assertThrows(
                        AccessTokenRequestException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString(OAuth2Error.UNSUPPORTED_GRANT_TYPE_CODE));
    }

    @Test
    void
            shouldThrowExceptionWhenCreateTokenRequestWithAuthorizationFindZeroOrMoreThanOneMatchingAddressSessionItem() {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "code=%S&redirect_uri=%S&grant_type=authorization_code&client_id=test_client_id",
                        authCodeValue, redirectUri);

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString("Parameter must have exactly one value"));
    }

    @Test
    void shouldThrowInValidGrantExceptionWhenRedirectUriDoesNotMatchTheRetrievedItemRedirectUri() {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "code=%S&redirect_uri=%S&grant_type=authorization_code&client_id=test_client_id",
                        authCodeValue, redirectUri);

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockAddressSessionItem.getAuthorizationCode()).thenReturn(authCodeValue);
        when(mockAddressSessionItem.getRedirectUri())
                .thenReturn(URI.create("http://different-redirectUri"));
        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockDataStore.getItemByGsi(mockAuthorizationCodeIndex, authCodeValue))
                .thenReturn(List.of(mockAddressSessionItem));
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        AccessTokenRequestException exception =
                assertThrows(
                        AccessTokenRequestException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Requested redirectUri: HTTP://TEST.COM does not match existing redirectUri"));
        assertThat(exception.getErrorObject(), equalTo(OAuth2Error.INVALID_GRANT));
    }

    @Test
    void shouldCallWriteTokenAndUpdateDataStore() {
        AccessTokenResponse accessTokenResponse = mock(AccessTokenResponse.class);
        AddressSessionItem addressSessionItem = mock(AddressSessionItem.class);

        Tokens mockTokens = mock(Tokens.class);
        when(accessTokenResponse.getTokens()).thenReturn(mockTokens);
        BearerAccessToken mockBearerAccessToken = mock(BearerAccessToken.class);
        when(mockTokens.getBearerAccessToken()).thenReturn(mockBearerAccessToken);
        when(mockBearerAccessToken.toAuthorizationHeader()).thenReturn("some-authorization-header");
        accessTokenService.writeToken(accessTokenResponse, addressSessionItem);

        verify(mockDataStore).update(addressSessionItem);
        assertThat(accessTokenResponse, notNullValue());
    }

    @Test
    void shouldCallCreateTokenWithATokenRequest() {
        TokenRequest tokenRequest = mock(TokenRequest.class);
        TokenResponse tokenResponse = accessTokenService.createToken(tokenRequest);

        verify(tokenRequest).getScope();
        assertThat(tokenResponse, notNullValue());
    }
}
