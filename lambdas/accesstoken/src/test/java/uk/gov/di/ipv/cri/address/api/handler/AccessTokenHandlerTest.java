package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.AccessTokenValidationException;
import uk.gov.di.ipv.cri.address.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.address.library.service.AccessTokenService;
import uk.gov.di.ipv.cri.address.library.service.SessionService;
import uk.gov.di.ipv.cri.address.library.util.EventProbe;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.AccessTokenHandler.METRIC_NAME_ACCESS_TOKEN;

@ExtendWith(MockitoExtension.class)
class AccessTokenHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock private EventProbe eventProbe;
    @Mock private AccessTokenService mockAccessTokenService;
    @Mock private SessionService mockSessionService;
    @Mock private TokenRequest tokenRequest;

    private AccessTokenHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AccessTokenHandler(mockAccessTokenService, mockSessionService, eventProbe);
    }

    @Test
    void shouldReturnAccessTokenOnSuccessfulExchange() throws Exception {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
                                + "&client_id=urn:uuid:ipv-core"
                                + "&grant_type=%s",
                        authCodeValue, "jwt-string", grantType);
        event.withBody(tokenRequestBody);
        AccessTokenResponse tokenResponse = createTestTokenResponse();
        SessionItem mockSessionItem = mock(SessionItem.class);

        when(mockAccessTokenService.createTokenRequest(tokenRequestBody)).thenReturn(tokenRequest);
        when(mockAccessTokenService.getAuthorizationCode(tokenRequest)).thenReturn(authCodeValue);
        when(mockAccessTokenService.createToken(tokenRequest)).thenReturn(tokenResponse);
        when(mockSessionService.getSessionByAuthorisationCode(authCodeValue))
                .thenReturn(mockSessionItem);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        Map<String, Object> responseBody =
                objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals(
                tokenResponse.toSuccessResponse().getTokens().getAccessToken().getValue(),
                responseBody.get("access_token").toString());

        verify(eventProbe).counterMetric(METRIC_NAME_ACCESS_TOKEN);
    }

    @Test
    void shouldReturn400WhenCannotCreateTokenRequest()
            throws AccessTokenValidationException, JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withBody("some body");

        AccessTokenValidationException exception =
                new AccessTokenValidationException("an error message");
        when(mockAccessTokenService.createTokenRequest("some body")).thenThrow(exception);
        when(eventProbe.log(Level.ERROR, exception)).thenReturn(eventProbe);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertErrorResponse(response, ErrorResponse.TOKEN_VALIDATION_ERROR);
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric(METRIC_NAME_ACCESS_TOKEN, 0d);
        verifyNoMoreInteractions(mockAccessTokenService);
    }

    @Test
    void shouldReturn400WhenCannotValidateTokenRequest()
            throws AccessTokenValidationException, JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.withBody("some body");
        SessionItem mockSessionItem = mock(SessionItem.class);
        String authCode = String.valueOf(UUID.randomUUID());
        AccessTokenValidationException exception =
                new AccessTokenValidationException("an error message");
        when(mockAccessTokenService.createTokenRequest("some body")).thenReturn(tokenRequest);
        when(mockAccessTokenService.getAuthorizationCode(tokenRequest)).thenReturn(authCode);
        when(mockSessionService.getSessionByAuthorisationCode(authCode))
                .thenReturn(mockSessionItem);
        when(mockAccessTokenService.validateTokenRequest(tokenRequest, mockSessionItem))
                .thenThrow(exception);
        when(eventProbe.log(Level.ERROR, exception)).thenReturn(eventProbe);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertErrorResponse(response, ErrorResponse.TOKEN_VALIDATION_ERROR);
        verify(eventProbe).log(Level.ERROR, exception);
        verify(eventProbe).counterMetric(METRIC_NAME_ACCESS_TOKEN, 0d);
        verifyNoMoreInteractions(mockAccessTokenService);
    }

    private AccessTokenResponse createTestTokenResponse() {
        AccessToken accessToken =
                new BearerAccessToken(
                        Duration.of(1, ChronoUnit.HOURS).toSeconds(), Scope.parse("some-scope"));
        return new AccessTokenResponse(new Tokens(accessToken, null)).toSuccessResponse();
    }

    private void assertErrorResponse(
            APIGatewayProxyResponseEvent response, ErrorResponse errorResponse)
            throws JsonProcessingException {
        Map responseBody = new ObjectMapper().readValue(response.getBody(), Map.class);
        assertEquals(errorResponse.getCode(), responseBody.get("code"));
        assertEquals(errorResponse.getMessage(), responseBody.get("message"));
    }
}
