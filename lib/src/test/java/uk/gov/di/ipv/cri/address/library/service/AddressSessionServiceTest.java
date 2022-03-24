package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import uk.gov.di.ipv.cri.address.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenRequestException;
import uk.gov.di.ipv.cri.address.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.address.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressSessionServiceTest {

    private static Instant fixedInstant;

    private AddressSessionService addressSessionService;

    @Mock private DataStore<AddressSessionItem> mockDataStore;

    @Mock private ConfigurationService mockConfigurationService;

    @Captor private ArgumentCaptor<AddressSessionItem> mockAddressSessionItem;

    @Test
    void shouldCallCreateOnAddressSessionDataStore() {
        when(mockConfigurationService.getAddressSessionTtl()).thenReturn(1L);
        SessionRequest sessionRequest = mock(SessionRequest.class);

        when(sessionRequest.getClientId()).thenReturn("a client id");
        when(sessionRequest.getState()).thenReturn("state");
        when(sessionRequest.getRedirectUri())
                .thenReturn(URI.create("https://www.example.com/callback"));

        addressSessionService.createAndSaveAddressSession(sessionRequest);
        verify(mockDataStore).create(mockAddressSessionItem.capture());
        AddressSessionItem capturedValue = mockAddressSessionItem.getValue();
        assertNotNull(capturedValue.getSessionId());
        assertThat(capturedValue.getExpiryDate(), equalTo(fixedInstant.getEpochSecond() + 1));
        assertThat(capturedValue.getClientId(), equalTo("a client id"));
        assertThat(capturedValue.getState(), equalTo("state"));
        assertThat(
                capturedValue.getRedirectUri(),
                equalTo(URI.create("https://www.example.com/callback")));
    }

    @Test
    void shouldThrowValidationExceptionWhenSessionRequestIsInvalid() {

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(Map.of("not", "a-session-request")));
                        });
        assertThat(exception.getMessage(), containsString("could not parse request body"));
        verifyNoInteractions(mockConfigurationService);
    }

    @Test
    void shouldThrowValidationExceptionWhenRequestClientIdIsInvalid() {

        SessionRequestBuilder sessionRequestBuilder =
                new SessionRequestBuilder().withClientId("bad-client-id");
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);
        when(mockConfigurationService.getParametersForPath(
                        "/clients/bad-client-id/jwtAuthentication"))
                .thenReturn(Map.of());
        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(sessionRequest));
                        });
        assertThat(exception.getMessage(), containsString("no configuration for client id"));
        verify(mockConfigurationService)
                .getParametersForPath("/clients/bad-client-id/jwtAuthentication");
    }

    @Test
    void shouldThrowValidationExceptionWhenRequestUriIsInvalid() {

        SessionRequestBuilder sessionRequestBuilder =
                new SessionRequestBuilder()
                        .withRedirectUri(URI.create("https://www.example.com/not-valid-callback"));
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));
        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(sessionRequest));
                        });
        assertThat(
                exception.getMessage(),
                containsString(
                        "redirect uri https://www.example.com/not-valid-callback does not match configuration uri https://www.example/com/callback"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTIsInvalid() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);
        sessionRequest.setRequestJWT(
                Base64.getEncoder().encodeToString("not a jwt".getBytes(StandardCharsets.UTF_8)));

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));
        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(sessionRequest));
                        });
        assertThat(exception.getMessage(), containsString("Could not parse request JWT"));
    }

    @Test
    void shouldThrowValidationExceptionWhenClientX509CertDoesNotMatchPrivateKey() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder()
                        .setCertificateFile("wrong-cert.crt.pem");
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(sessionRequest));
                        });

        assertThat(exception.getMessage(), containsString("JWT signature verification failed"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTHeaderDoesNotMatchConfig() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder()
                        .setSigningAlgorithm(JWSAlgorithm.RS512);
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(sessionRequest));
                        });

        assertThat(
                exception.getMessage(),
                containsString(
                        "jwt signing algorithm RS512 does not match signing algorithm configured for client: RS256"));
    }

    @Test
    void shouldThrowValidationExceptionWhenJWTIsExpired() {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder()
                        .setNow(Instant.now().minus(1, ChronoUnit.DAYS));
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        SessionValidationException exception =
                assertThrows(
                        SessionValidationException.class,
                        () -> {
                            addressSessionService.validateSessionRequest(
                                    marshallToJSON(sessionRequest));
                        });

        assertThat(exception.getMessage(), containsString("could not parse JWT"));
    }

    @Test
    void shouldValidateJWTSignedWithRSAKey()
            throws IOException, SessionValidationException, ClientConfigurationException {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(standardSSMConfigMap(signedJWTBuilder));

        SessionRequest result =
                addressSessionService.validateSessionRequest(marshallToJSON(sessionRequest));
        assertThat(result.getState(), equalTo(sessionRequest.getState()));
        assertThat(result.getClientId(), equalTo(sessionRequest.getClientId()));
        assertThat(result.getRedirectUri(), equalTo(sessionRequest.getRedirectUri()));
        assertThat(result.getResponseType(), equalTo(sessionRequest.getResponseType()));
    }

    @Test
    void shouldValidateJWTSignedWithECKey()
            throws IOException, SessionValidationException, ClientConfigurationException {
        SessionRequestBuilder sessionRequestBuilder = new SessionRequestBuilder();
        SessionRequestBuilder.SignedJWTBuilder signedJWTBuilder =
                new SessionRequestBuilder.SignedJWTBuilder();
        signedJWTBuilder.setPrivateKeyFile("signing_ec.pk8");
        signedJWTBuilder.setCertificateFile("signing_ec.crt.pem");
        signedJWTBuilder.setSigningAlgorithm(JWSAlgorithm.ES384);
        SessionRequest sessionRequest = sessionRequestBuilder.build(signedJWTBuilder);

        Map<String, String> configMap = standardSSMConfigMap(signedJWTBuilder);
        configMap.put("authenticationAlg", "ES384");
        when(mockConfigurationService.getParametersForPath("/clients/ipv-core/jwtAuthentication"))
                .thenReturn(configMap);

        SessionRequest result =
                addressSessionService.validateSessionRequest(marshallToJSON(sessionRequest));
        assertThat(result.getState(), equalTo(sessionRequest.getState()));
        assertThat(result.getClientId(), equalTo(sessionRequest.getClientId()));
        assertThat(result.getRedirectUri(), equalTo(sessionRequest.getRedirectUri()));
        assertThat(result.getResponseType(), equalTo(sessionRequest.getResponseType()));
    }

    @Test
    void shouldCallCreateTokenRequestSuccessfully() throws com.nimbusds.oauth2.sdk.ParseException {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "code=%S&redirect_uri=%S&grant_type=authorization_code&client_id=test_client_id",
                        authCodeValue, redirectUri);
        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);
        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        Page<AddressSessionItem> item = Page.create(List.of(mockAddressSessionItem));
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(item);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);
        when(mockAuthorizationCodeIndex.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);
        when(mockAddressSessionItem.getAuthorizationCode()).thenReturn(authCodeValue);
        when(mockAddressSessionItem.getRedirectUri()).thenReturn(URI.create(redirectUri));
        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);

        TokenRequest tokenRequest = addressSessionService.createTokenRequest(tokenRequestBody);
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
                        () -> addressSessionService.createTokenRequest(tokenRequestBody));

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

        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);
        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        Page<AddressSessionItem> item = Page.create(List.of(mockAddressSessionItem));
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(item);

        when(mockAddressSessionItem.getAuthorizationCode()).thenReturn("wrong-authorization-code");
        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);
        when(mockAuthorizationCodeIndex.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);

        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);
        AccessTokenRequestException exception =
                assertThrows(
                        AccessTokenRequestException.class,
                        () -> addressSessionService.createTokenRequest(tokenRequestBody));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Cannot for the Address session item for the given authorization Code"));
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

        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);
        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        Page<AddressSessionItem> item = Page.create(List.of(mockAddressSessionItem));
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(item);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);
        when(mockAuthorizationCodeIndex.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);

        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);
        AccessTokenRequestException exception =
                assertThrows(
                        AccessTokenRequestException.class,
                        () -> addressSessionService.createTokenRequest(tokenRequestBody));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Cannot for the Address session item for the given authorization Code"));
        assertThat(exception.getErrorObject(), equalTo(OAuth2Error.INVALID_GRANT));
    }

    @Test
    void shouldThrowInValidGrantExceptionWhenRedirectUriDoesNotMatchTheRetrievedItemRedirectUri() {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "code=%S&redirect_uri=%S&grant_type=authorization_code&client_id=test_client_id",
                        authCodeValue, redirectUri);

        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);
        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        Page<AddressSessionItem> item = Page.create(List.of(mockAddressSessionItem));
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(item);

        when(mockAddressSessionItem.getAuthorizationCode()).thenReturn(authCodeValue);
        when(mockAddressSessionItem.getRedirectUri())
                .thenReturn(URI.create("http://different-redirectUri"));
        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);
        when(mockAuthorizationCodeIndex.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);

        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);
        AccessTokenRequestException exception =
                assertThrows(
                        AccessTokenRequestException.class,
                        () -> addressSessionService.createTokenRequest(tokenRequestBody));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Requested redirectUri: HTTP://TEST.COM does not match existing redirectUri"));
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

        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);
        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        Page<AddressSessionItem> item = Page.create(List.of(mockAddressSessionItem));
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(item);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);
        when(mockAuthorizationCodeIndex.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);

        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);
        AccessTokenRequestException exception =
                assertThrows(
                        AccessTokenRequestException.class,
                        () -> addressSessionService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString(OAuth2Error.UNSUPPORTED_GRANT_TYPE_CODE));
    }

    @Test
    void shouldThrowExceptionCreateTokenRequestSuccessfully() {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String tokenRequestBody =
                String.format(
                        "code=%S&redirect_uri=%S&grant_type=authorization_code&client_id=test_client_id",
                        authCodeValue, redirectUri);
        var attVal = AttributeValue.builder().s(authCodeValue).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        AddressSessionItem mockAddressSessionItem = mock(AddressSessionItem.class);
        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockAuthorizationCodeIndex = mock(DynamoDbIndex.class);
        SdkIterable<Page<AddressSessionItem>> pageSdkIterableMock = mock(SdkIterable.class);
        Page<AddressSessionItem> item =
                Page.create(List.of(mockAddressSessionItem, mockAddressSessionItem));
        Stream<Page<AddressSessionItem>> streamedItem = Stream.of(item);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.AUTHORIZATION_CODE_INDEX))
                .thenReturn(mockAuthorizationCodeIndex);
        when(mockAuthorizationCodeIndex.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .thenReturn(pageSdkIterableMock);
        when(pageSdkIterableMock.stream()).thenReturn(streamedItem);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> addressSessionService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString("Parameter must have exactly one value"));
    }

    private Map<String, String> standardSSMConfigMap(
            SessionRequestBuilder.SignedJWTBuilder builder) {
        try {

            HashMap<String, String> map = new HashMap<>();
            map.put("redirectUri", "https://www.example/com/callback");
            map.put("authenticationAlg", "RS256");
            map.put("issuer", "ipv-core");
            map.put(
                    "publicCertificateToVerify",
                    Base64.getEncoder().encodeToString(builder.getCertificate().getEncoded()));
            return map;
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void shouldCallCreateTokenWithATokenRequest() {
        TokenRequest tokenRequest = mock(TokenRequest.class);
        TokenResponse tokenResponse = addressSessionService.createToken(tokenRequest);

        verify(tokenRequest).getScope();
        assertThat(tokenResponse, notNullValue());
    }

    @Test
    void shouldCallwriteTokenAndUpdateDataStore() {
        AccessTokenResponse accessTokenResponse = mock(AccessTokenResponse.class);
        AddressSessionItem addressSessionItem = mock(AddressSessionItem.class);

        Tokens mockTokens = mock(Tokens.class);
        when(accessTokenResponse.getTokens()).thenReturn(mockTokens);
        BearerAccessToken mockBearerAccessToken = mock(BearerAccessToken.class);
        when(mockTokens.getBearerAccessToken()).thenReturn(mockBearerAccessToken);
        when(mockBearerAccessToken.toAuthorizationHeader()).thenReturn("some-authorization-header");
        addressSessionService.writeToken(accessTokenResponse, addressSessionItem);

        verify(mockDataStore).update(addressSessionItem);
        assertThat(accessTokenResponse, notNullValue());
    }

    private String marshallToJSON(Object sessionRequest) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(sessionRequest);
    }

    @BeforeAll
    static void beforeAll() {
        fixedInstant = Instant.now();
    }

    @BeforeEach
    void setUp() {
        Clock nowClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        addressSessionService =
                new AddressSessionService(mockDataStore, mockConfigurationService, nowClock);
    }
}
