package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;
import uk.gov.di.ipv.cri.address.library.service.AddressSessionService;
import uk.gov.di.ipv.cri.address.library.service.ConfigurationService;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock private Context context;
    private EventProbe eventProbe = new EventProbe();
    @Mock private AddressSessionService mockAddressSessionService;

    private AccessTokenHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AccessTokenHandler(mockAddressSessionService, eventProbe);
    }

    @Test
    void shouldReturnAccessTokenOnSuccessfulExchange() throws Exception {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        String tokenRequestBody =
                "code=12345&redirect_uri=http://test.com&grant_type=authorization_code&client_id=test_client_id";
        event.withBody(tokenRequestBody);
        AddressSessionItem addressSessionItem = mock(AddressSessionItem.class);
        AccessToken accessToken = new BearerAccessToken();
        TokenResponse tokenResponse = new AccessTokenResponse(new Tokens(accessToken, null));

        // TODO: This here as a placeholder pending the story that generates the authorization code
        TokenRequest tokenRequest = mock(TokenRequest.class);
        when(tokenRequest.getAuthorizationGrant())
                .thenReturn(
                        new AuthorizationCodeGrant(
                                new AuthorizationCode("12345"),
                                URI.create("http://test.com"),
                                null));
        when(mockAddressSessionService.createTokenRequest(tokenRequestBody))
                .thenReturn(tokenRequest);
        when(mockAddressSessionService.createToken(any())).thenReturn(tokenResponse);
        when(mockAddressSessionService.getAddressSessionItemByValue(any()))
                .thenReturn(addressSessionItem);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        Map<String, Object> responseBody =
                objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(
                tokenResponse.toSuccessResponse().getTokens().getAccessToken().getValue(),
                responseBody.get("access_token").toString());
    }

    @Test
    void shouldReturn400WhenInvalidTokenRequestProvided() throws Exception {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AddressSessionService spyAddressSessionService =
                Mockito.spy(
                        new AddressSessionService(
                                (DataStore<AddressSessionItem>) mock(DataStore.class),
                                mock(ConfigurationService.class),
                                Clock.fixed(Instant.now(), ZoneId.systemDefault())));
        String invalidTokenRequest = "invalid-token-request";
        event.withBody(invalidTokenRequest);

        handler = new AccessTokenHandler(spyAddressSessionService, eventProbe);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        ErrorObject errorResponse = createErrorObjectFromResponse(response.getBody());

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals(OAuth2Error.INVALID_REQUEST.getCode(), errorResponse.getCode());
        assertEquals(OAuth2Error.INVALID_REQUEST.getDescription(), errorResponse.getDescription());
    }

    @Test
    void shouldReturn400WhenInvalidGrantTypeProvided() throws Exception {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        String tokenRequestBody =
                "code=12345&redirect_uri=http://test.com&grant_type="
                        + GrantType.IMPLICIT.getValue()
                        + "&client_id=test_client_id";

        event.withBody(tokenRequestBody);
        when(mockAddressSessionService.createTokenRequest(tokenRequestBody))
                .thenThrow(new AccessTokenRequestException(OAuth2Error.UNSUPPORTED_GRANT_TYPE));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        ErrorObject errorResponse = createErrorObjectFromResponse(response.getBody());

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals(OAuth2Error.UNSUPPORTED_GRANT_TYPE_CODE, errorResponse.getCode());
        assertEquals(
                OAuth2Error.UNSUPPORTED_GRANT_TYPE.getDescription(),
                errorResponse.getDescription());
    }

    @Test
    void shouldReturn400WhenInvalidRedirectUriIsProvided() throws ParseException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        String tokenRequestBody =
                "code=12345&redirect_uri=http://test.com&grant_type=authorization_code&client_id=test_client_id";

        event.withBody(tokenRequestBody);
        when(mockAddressSessionService.createTokenRequest(tokenRequestBody))
                .thenThrow(new AccessTokenRequestException(OAuth2Error.INVALID_GRANT));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        ErrorObject errorResponse = createErrorObjectFromResponse(response.getBody());

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals(OAuth2Error.INVALID_GRANT.getCode(), errorResponse.getCode());
        assertEquals(OAuth2Error.INVALID_GRANT.getDescription(), errorResponse.getDescription());
    }

    @Test
    void shouldReturn400WhenInvalidAuthorisationCodeProvided() throws Exception {
        String tokenRequestBody =
                "code=12345&redirect_uri=http://test.com&grant_type=authorization_code&client_id=test_client_id";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withBody(tokenRequestBody);

        when(mockAddressSessionService.createTokenRequest(tokenRequestBody))
                .thenThrow(new AccessTokenRequestException(OAuth2Error.INVALID_GRANT));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        ErrorObject errorResponse = createErrorObjectFromResponse(response.getBody());

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals(OAuth2Error.INVALID_GRANT.getCode(), errorResponse.getCode());
        assertEquals(OAuth2Error.INVALID_GRANT.getDescription(), errorResponse.getDescription());
    }

    private ErrorObject createErrorObjectFromResponse(String responseBody) throws ParseException {
        HTTPResponse httpErrorResponse = new HTTPResponse(HttpStatus.SC_BAD_REQUEST);
        httpErrorResponse.setContentType(ContentType.APPLICATION_JSON.getType());
        httpErrorResponse.setContent(responseBody);
        return ErrorObject.parse(httpErrorResponse);
    }
}
