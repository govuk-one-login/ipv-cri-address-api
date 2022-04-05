package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import uk.gov.di.ipv.cri.address.library.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.library.helpers.EventProbe;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;
import uk.gov.di.ipv.cri.address.library.service.CredentialIssuerService;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.IssueCredentialHandler.ADDRESS_CREDENTIAL_ISSUER;

@ExtendWith(MockitoExtension.class)
class IssueCredentialHandlerTest {
    @Mock private Context context;
    @Mock private CredentialIssuerService mockAddressCredentialIssuerService;
    @Mock private EventProbe eventProbe;
    @InjectMocks IssueCredentialHandler handler;

    @Test
    void shouldReturnAddressAndAOneValueWhenIssueCredentialRequestIsValid()
            throws CredentialRequestException, ParseException {
        UUID sessionId = UUID.randomUUID();
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        CredentialIssuerService.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));
        when(mockAddressCredentialIssuerService.getSessionId(event)).thenReturn(sessionId);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockAddressCredentialIssuerService).getAddresses(sessionId);
        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertEquals("1", response.getBody());
        verify(eventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenSubjectIsNotSupplied()
            throws CredentialRequestException, ParseException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        CredentialIssuerService.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));
        setupEventProbeErrorBehaviour();
        event.withHeaders(
                Map.of(
                        CredentialIssuerService.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        when(mockAddressCredentialIssuerService.getSessionId(event))
                .thenThrow(CredentialRequestException.class);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals("0", response.getBody());
        verify(eventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenAuthorizationHeaderIsNotSupplied()
            throws CredentialRequestException, ParseException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        setupEventProbeErrorBehaviour();

        when(mockAddressCredentialIssuerService.getSessionId(event))
                .thenThrow(CredentialRequestException.class);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals("0", response.getBody());
        verify(eventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
    }

    @Test
    void shouldThrowInterServerErrorWhenThereIsAnAWSError() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        CredentialIssuerService.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setupEventProbeErrorBehaviour();
        DataStore<AddressSessionItem> mockDataStore = mock(DataStore.class);
        CredentialIssuerService spyCredentialIssuerService =
                spy(new CredentialIssuerService(mockDataStore));

        DynamoDbTable<AddressSessionItem> mockAddressSessionTable = mock(DynamoDbTable.class);
        DynamoDbIndex<AddressSessionItem> mockTokenIndex = mock(DynamoDbIndex.class);

        when(mockDataStore.getTable()).thenReturn(mockAddressSessionTable);
        when(mockAddressSessionTable.index(AddressSessionItem.ACCESS_TOKEN_INDEX))
                .thenReturn(mockTokenIndex);

        AwsErrorDetails awsErrorDetails =
                AwsErrorDetails.builder()
                        .errorCode("")
                        .sdkHttpResponse(
                                SdkHttpResponse.builder()
                                        .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                        .build())
                        .errorMessage("AWS DynamoDbException Occurred")
                        .build();

        when(mockDataStore.getItemByGsi(mockTokenIndex, accessToken.toAuthorizationHeader()))
                .thenThrow(
                        DynamoDbException.builder()
                                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                .awsErrorDetails(awsErrorDetails)
                                .build());

        handler = new IssueCredentialHandler(spyCredentialIssuerService, eventProbe);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        String responseBody = new ObjectMapper().readValue(response.getBody(), String.class);
        assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), response.getStatusCode());
        assertEquals(awsErrorDetails.errorMessage(), responseBody);
        verify(eventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
    }

    private void setupEventProbeErrorBehaviour() {
        when(eventProbe.counterMetric(anyString(), anyDouble())).thenReturn(eventProbe);
        when(eventProbe.log(any(Level.class), any(Exception.class))).thenReturn(eventProbe);
    }
}
