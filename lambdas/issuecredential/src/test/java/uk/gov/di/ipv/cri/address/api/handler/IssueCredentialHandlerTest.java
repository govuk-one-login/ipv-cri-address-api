package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
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
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;
import uk.gov.di.ipv.cri.address.api.service.VerifiableCredentialService;
import uk.gov.di.ipv.cri.address.library.domain.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;
import uk.gov.di.ipv.cri.address.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.address.library.service.AddressService;
import uk.gov.di.ipv.cri.address.library.service.SessionService;
import uk.gov.di.ipv.cri.address.library.util.EventProbe;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.IssueCredentialHandler.ADDRESS_CREDENTIAL_ISSUER;

@ExtendWith(MockitoExtension.class)
class IssueCredentialHandlerTest {
    public static final String SUBJECT = "subject";
    @Mock private Context context;
    @Mock private VerifiableCredentialService mockVerifiableCredentialService;
    @Mock private SessionService mockSessionService;

    @Mock private AddressService mockAddressService;
    @Mock private EventProbe mockEventProbe;
    @InjectMocks private IssueCredentialHandler handler;

    @Test
    void shouldReturn200OkWhenIssueCredentialRequestIsValid() throws JOSEException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));
        setRequestBodyAsPlainJWT(event);

        final UUID sessionId = UUID.randomUUID();
        CanonicalAddress address = new CanonicalAddress();
        address.setBuildingNumber("114");
        address.setStreetName("Wellington Street");
        address.setPostalCode("LS1 1BA");
        AddressItem addressItem = new AddressItem();
        List<CanonicalAddress> canonicalAddresses = List.of(address);

        SessionItem sessionItem = new SessionItem();
        sessionItem.setSubject(SUBJECT);
        sessionItem.setSessionId(sessionId);
        addressItem.setAddresses(canonicalAddresses);

        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(sessionItem);
        when(mockAddressService.getAddressItem(sessionId)).thenReturn(addressItem);
        when(mockVerifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        SUBJECT, canonicalAddresses))
                .thenReturn(mock(SignedJWT.class));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockAddressService).getAddressItem(sessionId);
        verify(mockVerifiableCredentialService)
                .generateSignedVerifiableCredentialJwt(SUBJECT, canonicalAddresses);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

        assertEquals(
                ContentType.APPLICATION_JWT.getType(), response.getHeaders().get("Content-Type"));
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    }

    @Test
    void shouldThrowJOSEExceptionWhenGenerateVerifiableCredentialIsMalformed()
            throws JsonProcessingException, JOSEException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));
        setRequestBodyAsPlainJWT(event);
        setupEventProbeErrorBehaviour();
        var unExpectedJOSEException = new JOSEException("Unexpected JOSE object type: JWSObject");

        final UUID sessionId = UUID.randomUUID();
        CanonicalAddress address = new CanonicalAddress();
        address.setBuildingNumber("114");
        address.setStreetName("Wellington Street");
        address.setPostalCode("LS1 1BA");
        AddressItem addressItem = new AddressItem();
        List<CanonicalAddress> canonicalAddresses = List.of(address);

        SessionItem sessionItem = new SessionItem();
        sessionItem.setSubject(SUBJECT);
        sessionItem.setSessionId(sessionId);
        addressItem.setAddresses(canonicalAddresses);

        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(sessionItem);
        when(mockAddressService.getAddressItem(sessionId)).thenReturn(addressItem);
        when(mockVerifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        SUBJECT, canonicalAddresses))
                .thenThrow(unExpectedJOSEException);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockAddressService).getAddressItem(sessionId);
        verify(mockVerifiableCredentialService)
                .generateSignedVerifiableCredentialJwt(SUBJECT, canonicalAddresses);
        verify(mockEventProbe).log(Level.ERROR, unExpectedJOSEException);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
        verifyNoMoreInteractions(mockVerifiableCredentialService);

        Map responseBody = new ObjectMapper().readValue(response.getBody(), Map.class);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorResponse.VERIFIABLE_CREDENTIAL_ERROR.getCode(), responseBody.get("code"));
        assertEquals(
                ErrorResponse.VERIFIABLE_CREDENTIAL_ERROR.getMessage(),
                responseBody.get("message"));
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenAuthorizationHeaderIsNotSupplied() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        setupEventProbeErrorBehaviour();

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldThrowAWSExceptionWhenAServerErrorOccursRetrievingASessionItemWithAccessToken()
            throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setRequestBodyAsPlainJWT(event);
        setupEventProbeErrorBehaviour();

        AwsErrorDetails awsErrorDetails =
                AwsErrorDetails.builder()
                        .errorCode("")
                        .sdkHttpResponse(
                                SdkHttpResponse.builder()
                                        .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                        .build())
                        .errorMessage("AWS DynamoDbException Occurred")
                        .build();

        when(mockSessionService.getSessionByAccessToken(accessToken))
                .thenThrow(
                        AwsServiceException.builder()
                                .statusCode(500)
                                .awsErrorDetails(awsErrorDetails)
                                .build());

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

        String responseBody = new ObjectMapper().readValue(response.getBody(), String.class);
        assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), response.getStatusCode());
        assertEquals(awsErrorDetails.errorMessage(), responseBody);
    }

    @Test
    void shouldThrowAWSExceptionWhenAServerErrorOccursDuringRetrievingAnAddressItemWithSessionId()
            throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setRequestBodyAsPlainJWT(event);
        setupEventProbeErrorBehaviour();

        AwsErrorDetails awsErrorDetails =
                AwsErrorDetails.builder()
                        .errorCode("")
                        .sdkHttpResponse(
                                SdkHttpResponse.builder()
                                        .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                        .build())
                        .errorMessage("AWS DynamoDbException Occurred")
                        .build();

        final UUID sessionId = UUID.randomUUID();
        SessionItem mockSessionItem = mock(SessionItem.class);
        when(mockSessionItem.getSessionId()).thenReturn(sessionId);
        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(mockSessionItem);
        when(mockAddressService.getAddressItem(sessionId))
                .thenThrow(
                        AwsServiceException.builder()
                                .statusCode(500)
                                .awsErrorDetails(awsErrorDetails)
                                .build());

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockAddressService).getAddressItem(sessionId);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

        String responseBody = new ObjectMapper().readValue(response.getBody(), String.class);
        assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), response.getStatusCode());
        assertEquals(awsErrorDetails.errorMessage(), responseBody);
    }

    private void setupEventProbeErrorBehaviour() {
        when(mockEventProbe.counterMetric(anyString(), anyDouble())).thenReturn(mockEventProbe);
        when(mockEventProbe.log(any(Level.class), any(Exception.class))).thenReturn(mockEventProbe);
    }

    private void setRequestBodyAsPlainJWT(APIGatewayProxyRequestEvent event) {
        String requestJWT =
                new PlainJWT(
                                new JWTClaimsSet.Builder()
                                        .claim(JWTClaimNames.SUBJECT, SUBJECT)
                                        .build())
                        .serialize();

        event.setBody(requestJWT);
    }
}
