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
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenValidationException;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {
    @Mock private DataStore<AddressSessionItem> mockDataStore;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private JWTVerifier mockJwtVerifier;
    private AccessTokenService accessTokenService;

    private final String SAMPLE_JWT = "eyJraWQiOiJpcHYtY29yZS1zdHViIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJ1cm46dXVpZDppcHYtY29yZSIsImF1ZCI6Imh0dHBzOlwvXC9leHBlcmlhbi5jcmkuYWNjb3VudC5nb3YudWsiLCJuYmYiOjE2NDk4NjExMDgsInNoYXJlZF9jbGFpbXMiOnsiYWRkcmVzc2VzIjpbeyJzdHJlZXQxIjoiOCIsInN0cmVldDIiOiJIQURMRVkgUk9BRCIsInRvd25DaXR5IjoiQkFUSCIsImN1cnJlbnRBZGRyZXNzIjp0cnVlLCJwb3N0Q29kZSI6IkJBMiA1QUEifV0sIm5hbWUiOlt7Im5hbWVQYXJ0cyI6W3sidmFsdWUiOiJLRU5ORVRIIiwidHlwZSI6IkdpdmVuTmFtZSJ9LHsidmFsdWUiOiJERUNFUlFVRUlSQSIsInR5cGUiOiJGYW1pbHlOYW1lIn1dfV0sImJpcnRoRGF0ZSI6W3sidmFsdWUiOiIxOTY0LTA5LTExIn1dLCJAY29udGV4dCI6WyJodHRwczpcL1wvd3d3LnczLm9yZ1wvMjAxOFwvY3JlZGVudGlhbHNcL3YxIiwiaHR0cHM6XC9cL3ZvY2FiLmxvbmRvbi5jbG91ZGFwcHMuZGlnaXRhbFwvY29udGV4dHNcL2lkZW50aXR5LXYxLmpzb25sZCJdfSwiaXNzIjoidXJuOnV1aWQ6aXB2LWNvcmUiLCJleHAiOjE2NDk4NjQ3MDgsImlhdCI6MTY0OTg2MTEwOCwianRpIjoiN2ZjMmI4NTYtN2Y5NS00NmI5LTk2ZmMtZGQ4NzBhZDE5MTAyIn0.QStBn6cCV_K_vVDpPS6wNzRdayQLabWxEywnwGYV7YaYwJ3CCPNDXVi72MAFrdf8a3-5SkES8oP_vXxCVi3Qxe2T_lAFKsOWsK_8-eN_wR_cQcgb4TR98s6Lc8QujZGWZLzlHR0Mmt5o-3z6tKtg1KMVXy35SlsTMvfUQENrznCTdoGdgs1x_DHfUr45sdBmL13mXqWWqUlh3ivKe9JBKHgWEXh8LcphbFlizBZSUYLGJDVOV88RsyFbM-JPB5Cqsu_cdBHV5BMoVtEZMjqW9XNtp3DI38RcqTrcP0R-xgIl33AUaRRueX1YnH1Qgs7YCd5i8RqotlaEMhK97ppRO16zz9a1rQ65y60GkU4z8Btcyr-LN-_QmcWMQCZaRI3h5khpRgxRFxAYYBqU6PtnK1g1Y6WqsXtoY90u0ybomhml-z_-UeMznYYUEmcbrM25uZa6ZJXGNa308d2MGziVSzi72xzX7srEW7gSj1-LWYgEdsY74zC7mHV2tCZGNIoZ4YeAxqXqAiNCzkP0ima-LzniCHCtwTFfC0H6VeTEmIGpUKUuuzx5tNNNrsYLVC3CZXSZTI5J_Zhn3z1VruvtmT4V8G8G7Lz0EkphdhZlhjelGOrTUwm9TUuype3XfHEgYkS1HSJF2IreQHLNfX1U953cYKAiqyQxeGJWfTr8I-0";

    @BeforeEach
    void setUp() {
        accessTokenService =
                new AccessTokenService(
                        mockDataStore,
                        Duration.ofHours(1).getSeconds(),
                        mockConfigurationService,
                        mockJwtVerifier);
    }

    @Test
    void shouldCallCreateTokenRequestSuccessfully() throws AccessTokenValidationException {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String tokenRequestBody =
                String.format(
                        "code=%s" +
                                "&client_assertion=%s" +
                                "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                                "&client_id=urn:uuid:ipv-core" +
                                "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);
        AuthorizationCodeGrant authorizationCodeGrant =
                (AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant();
        assertThat(tokenRequest, notNullValue());
        assertThat(
                authorizationCodeGrant.getAuthorizationCode().getValue(), equalTo(authCodeValue));
    }

    @Test
    void shouldThrowInvalidRequestWhenTokenRequestUrlParamsIsInComplete() {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String tokenRequestBody =
                String.format(
                        "code=%s" +
                                "&client_assertion=%s" +
                                "&client_id=urn:uuid:ipv-core" +
                                "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, grantType);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString(OAuth2Error.INVALID_REQUEST.getCode()));
    }

    @Test
    void shouldThrowExceptionWhenAddressSessionItemDoesNotExistForTheRequestedAuthorizationCode() {
        String authorizationCode = "12345";

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);

        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.getAddressSessionItemByAuthorizationCodeIndex(authorizationCode));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Cannot retrieve Address session item for the given authorization Code"));

    }

    @Test
    void shouldThrowExceptionWhenNoMatchingAddressSessionItemForTheRequestedAuthorizationCode() throws AccessTokenValidationException, SessionValidationException, ClientConfigurationException {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "urn:uuid:ipv-core";
        String tokenRequestBody =
                String.format(
                        "code=%s" +
                                "&client_assertion=%s" +
                                "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                                "&client_id=%s" +
                                "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, clientID, grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);

        AddressSessionItem addressSessionItem = new AddressSessionItem();
        addressSessionItem.setSessionId(UUID.randomUUID());
        addressSessionItem.setAuthorizationCode(authCodeValue);
        addressSessionItem.setClientId("123456789");

        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        when(mockAuthorizationCodeIndex.query(
                QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);

        Page<AddressSessionItem> item = Page.create(List.of(addressSessionItem));
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(item);
        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.validateTokenRequest(tokenRequest));

        assertThat(exception.getMessage(), containsString("issuer, sub, audience or jti are missing (or) request client id and saved client id do not match"));

    }

    @Test
    void shouldThrowUnSupportGrantTypeExceptionWhenAuthorizationGrantTypeIsInValid() {
        String grantType = "not_authorization_code";
        String tokenRequestBody =
                String.format("code=some-code" +
                                "&client_assertion=%s" +
                                "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                                "&client_id=urn:uuid:ipv-core" +
                                "&grant_type=%s",
                        SAMPLE_JWT, grantType);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString(OAuth2Error.UNSUPPORTED_GRANT_TYPE_CODE));
    }

    @Test
    void
            shouldThrowExceptionWhenCreateTokenRequestWithAuthorizationFindZeroOrMoreThanOneMatchingAddressSessionItem() throws AccessTokenValidationException, SessionValidationException, ClientConfigurationException {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "urn:uuid:ipv-core";
        String tokenRequestBody =
                String.format(
                        "code=%s" +
                                "&client_assertion=%s" +
                                "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                                "&client_id=%s" +
                                "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, clientID, grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);
        AddressSessionItem addressSessionItem1 = new AddressSessionItem();
        addressSessionItem1.setSessionId(UUID.randomUUID());
        addressSessionItem1.setAuthorizationCode(authCodeValue);
        addressSessionItem1.setClientId(clientID);

        AddressSessionItem addressSessionItem2 = new AddressSessionItem();
        addressSessionItem2.setSessionId(UUID.randomUUID());
        addressSessionItem2.setAuthorizationCode(authCodeValue);
        addressSessionItem2.setClientId(clientID);

        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        when(mockAuthorizationCodeIndex.query(
                QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);

        Page<AddressSessionItem> items = Page.create(List.of(addressSessionItem1, addressSessionItem2));
        Stream<Page<AddressSessionItem>> streamedItems = Stream.of(items);
        when(pageSdkIterableMock.stream()).thenReturn(streamedItems);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accessTokenService.validateTokenRequest(tokenRequest));

        assertThat(exception.getMessage(), containsString("Parameter must have exactly one value"));
        verify(mockJwtVerifier, never()).verifyJWTHeader(any(), any());
        verify(mockJwtVerifier, never()).verifyJWTClaimsSet(any(), any());
        verify(mockJwtVerifier, never()).verifyJWTSignature(any(), any());
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

    private Map<String, String> getSSMConfigMap() {
        try {

            HashMap<String, String> map = new HashMap<>();
            map.put("redirectUri", "https://www.example/com/callback");
            map.put("authenticationAlg", "RS256");
            map.put("issuer", "ipv-core");
            return map;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void shouldValidateTokenRequestSuccessfully() throws AccessTokenValidationException, SessionValidationException, ClientConfigurationException {

        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "urn:uuid:ipv-core";
        String tokenRequestBody =
                String.format(
                        "code=%s" +
                                "&client_assertion=%s" +
                                "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                                "&client_id=%s" +
                                "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, clientID, grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);
        AddressSessionItem addressSessionItem = new AddressSessionItem();
        addressSessionItem.setSessionId(UUID.randomUUID());
        addressSessionItem.setAuthorizationCode(authCodeValue);
        addressSessionItem.setClientId(clientID);

        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        when(mockAuthorizationCodeIndex.query(
                QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);

        Page<AddressSessionItem> item = Page.create(List.of(addressSessionItem));
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(item);
        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);

        when(mockConfigurationService.getParametersForPath(
                "/clients/"+clientID+"/jwtAuthentication")).thenReturn(getSSMConfigMap());

        TokenRequest expectedTokenRequest = accessTokenService.validateTokenRequest(tokenRequest);

        assertEquals(expectedTokenRequest.getClientID(), tokenRequest.getClientID());
        verify(mockJwtVerifier, times(1)).verifyJWTHeader(any(), any());
        verify(mockJwtVerifier, times(1)).verifyJWTClaimsSet(any(), any());
        verify(mockJwtVerifier, times(1)).verifyJWTSignature(any(), any());

    }

    @Test
    void shouldThrowExceptionForMissingClientConfiguration() throws AccessTokenValidationException, SessionValidationException, ClientConfigurationException {

        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "urn:uuid:ipv-core";
        String tokenRequestBody =
                String.format(
                        "code=%s" +
                                "&client_assertion=%s" +
                                "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                                "&client_id=%s" +
                                "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, clientID, grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);
        AddressSessionItem addressSessionItem = new AddressSessionItem();
        addressSessionItem.setSessionId(UUID.randomUUID());
        addressSessionItem.setAuthorizationCode(authCodeValue);
        addressSessionItem.setClientId(clientID);

        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        when(mockAuthorizationCodeIndex.query(
                QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);

        Page<AddressSessionItem> item = Page.create(List.of(addressSessionItem));
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(item);
        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);

        when(mockConfigurationService.getParametersForPath(
                "/clients/"+clientID+"/jwtAuthentication")).thenReturn(Map.of());

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () ->  accessTokenService.validateTokenRequest(tokenRequest));

        assertThat(exception.getMessage(), containsString("no configuration for client id '" + clientID + "'"));
        verify(mockJwtVerifier, never()).verifyJWTHeader(any(), any());
        verify(mockJwtVerifier, never()).verifyJWTClaimsSet(any(), any());
        verify(mockJwtVerifier, never()).verifyJWTSignature(any(), any());

    }

    @Test
    void shouldThrowParseExceptionWhenWrongAlgUsedForPrivateKeyJWT() throws AccessTokenValidationException, SessionValidationException, ClientConfigurationException {

        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "wrong-client-id";
        String tokenRequestBody =
                String.format(
                        "code=%s" +
                                "&client_assertion=%s" +
                                "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                                "&client_id=%s" +
                                "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, clientID, grantType);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () ->  accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString("Invalid private key JWT authentication: The client identifier doesn't match the client assertion subject / issuer"));
        verify(mockJwtVerifier, never()).verifyJWTHeader(any(), any());
        verify(mockJwtVerifier, never()).verifyJWTClaimsSet(any(), any());
        verify(mockJwtVerifier, never()).verifyJWTSignature(any(), any());

    }

    @Test
    void shouldThrowExceptionWhenAddressSessionItemNotFoundForATokenRequest() throws AccessTokenValidationException, SessionValidationException, ClientConfigurationException {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "urn:uuid:ipv-core";
        String tokenRequestBody =
                String.format(
                        "code=%s" +
                                "&client_assertion=%s" +
                                "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" +
                                "&client_id=%s" +
                                "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, clientID, grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->  accessTokenService.getAddressSession(tokenRequest));

        assertThat(exception.getMessage(), containsString("Parameter must have exactly one value"));
        verify(mockJwtVerifier, never()).verifyJWTHeader(any(), any());
        verify(mockJwtVerifier, never()).verifyJWTClaimsSet(any(), any());
        verify(mockJwtVerifier, never()).verifyJWTSignature(any(), any());

    }
}
