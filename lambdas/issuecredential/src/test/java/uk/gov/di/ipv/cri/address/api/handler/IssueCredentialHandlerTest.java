package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpResponse;
import uk.gov.di.ipv.cri.address.api.service.VerifiableCredentialService;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;
import uk.gov.di.ipv.cri.address.library.service.AddressService;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.logging.log4j.Level.INFO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.address.api.handler.IssueCredentialHandler.ADDRESS_CREDENTIAL_ISSUER;
import static uk.gov.di.ipv.cri.address.api.handler.IssueCredentialHandler.NO_SUCH_ALGORITHM_ERROR;
import static uk.gov.di.ipv.cri.address.api.objectmapper.CustomObjectMapper.getMapperWithCustomSerializers;
import static uk.gov.di.ipv.cri.address.api.service.fixtures.TestFixtures.EC_PRIVATE_KEY_1;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.VERIFIABLE_CREDENTIAL_ERROR;

@ExtendWith(MockitoExtension.class)
class IssueCredentialHandlerTest {
    public static final String SUBJECT = "subject";
    @Mock private Context context;
    @Mock private VerifiableCredentialService mockVerifiableCredentialService;
    @Mock private SessionService mockSessionService;
    @Mock private AddressService mockAddressService;
    @Mock private EventProbe mockEventProbe;
    @Mock private AuditService mockAuditService;

    @InjectMocks private IssueCredentialHandler handler;

    @Test
    void shouldReturn200OkWhenIssueCredentialRequestIsValid()
            throws JOSEException, SqsException, NoSuchAlgorithmException {
        ArgumentCaptor<AuditEventContext> endEventAuditEventContextArgCaptor =
                ArgumentCaptor.forClass(AuditEventContext.class);
        ArgumentCaptor<AuditEventContext> vcEventAuditEventContextArgCaptor =
                ArgumentCaptor.forClass(AuditEventContext.class);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));
        setRequestBodyAsPlainJWT(event);

        final UUID sessionId = UUID.randomUUID();
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSubject(SUBJECT);
        sessionItem.setSessionId(sessionId);

        CanonicalAddress address = new CanonicalAddress();
        address.setBuildingNumber("114");
        address.setStreetName("Wellington Street");
        address.setPostalCode("LS1 1BA");
        address.setAddressRegion("Dummy Region");
        AddressItem addressItem = new AddressItem();

        List<CanonicalAddress> canonicalAddresses = List.of(address);
        addressItem.setAddresses(canonicalAddresses);
        List<Address> addresses =
                canonicalAddresses.stream().map(Address::new).collect(Collectors.toList());

        Map<String, Object> auditEventExtensions =
                Map.of("iss", "issuer", "addressesEntered", addresses.size(), "isUkAddress", true);

        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(sessionItem);
        when(mockAddressService.getAddressItem(sessionId)).thenReturn(addressItem);
        when(mockVerifiableCredentialService.generateSignedVerifiableCredentialJwt(
                        SUBJECT, canonicalAddresses))
                .thenReturn(mock(SignedJWT.class));
        when(mockVerifiableCredentialService.getAuditEventExtensions(canonicalAddresses))
                .thenReturn(auditEventExtensions);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockAddressService).getAddressItem(sessionId);
        verify(mockVerifiableCredentialService)
                .generateSignedVerifiableCredentialJwt(SUBJECT, canonicalAddresses);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER);
        verify(mockEventProbe).log(INFO, "found session");
        verifyNoMoreInteractions(mockEventProbe);

        verify(mockAuditService)
                .sendAuditEvent(
                        eq(AuditEventType.VC_ISSUED),
                        vcEventAuditEventContextArgCaptor.capture(),
                        eq(auditEventExtensions));
        verify(mockAuditService)
                .sendAuditEvent(
                        eq(AuditEventType.END), endEventAuditEventContextArgCaptor.capture());

        AuditEventContext endAuditEventContext = endEventAuditEventContextArgCaptor.getValue();
        AuditEventContext vcAuditEventContext = vcEventAuditEventContextArgCaptor.getValue();

        assertEquals(HttpStatusCode.OK, response.getStatusCode());
        assertEquals(event.getHeaders(), endAuditEventContext.getRequestHeaders());
        assertEquals(sessionItem, endAuditEventContext.getSessionItem());
        assertEquals(sessionItem, endEventAuditEventContextArgCaptor.getValue().getSessionItem());
        assertEquals(
                ContentType.APPLICATION_JWT.getType(), response.getHeaders().get("Content-Type"));

        for (int i = 0; i < addresses.size(); i++) {
            Address addressInAuditContext =
                    vcAuditEventContext.getPersonIdentity().getAddresses().get(i);
            assertEquals(addresses.get(i).getUprn(), addressInAuditContext.getUprn());
            assertEquals(address.getBuildingNumber(), addressInAuditContext.getBuildingNumber());
            assertEquals(address.getStreetName(), addressInAuditContext.getStreetName());
            assertEquals(address.getAddressLocality(), addressInAuditContext.getAddressLocality());
            assertEquals(address.getPostalCode(), addressInAuditContext.getPostalCode());
            assertEquals(address.getAddressCountry(), addressInAuditContext.getAddressCountry());
            assertEquals(address.getAddressRegion(), addressInAuditContext.getAddressRegion());
        }
    }

    @Test
    void shouldReturn200OkWhenIssueCredentialRequestGeneratesClaimsSetJwt()
            throws JOSEException, SqsException, InvalidKeySpecException, NoSuchAlgorithmException {
        ArgumentCaptor<AuditEventContext> endEventAuditEventContextArgCaptor =
                ArgumentCaptor.forClass(AuditEventContext.class);
        ArgumentCaptor<AuditEventContext> vcEventAuditEventContextArgCaptor =
                ArgumentCaptor.forClass(AuditEventContext.class);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));
        setRequestBodyAsPlainJWT(event);

        final UUID sessionId = UUID.randomUUID();
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSubject(SUBJECT);
        sessionItem.setSessionId(sessionId);

        CanonicalAddress address = new CanonicalAddress();
        address.setBuildingNumber("114");
        address.setStreetName("Wellington Street");
        address.setPostalCode("LS1 1BA");
        address.setAddressRegion("Dummy Region");
        address.setAddressCountry("GB");
        AddressItem addressItem = new AddressItem();

        List<CanonicalAddress> canonicalAddresses = List.of(address);
        addressItem.setAddresses(canonicalAddresses);
        List<Address> addresses =
                canonicalAddresses.stream().map(Address::new).collect(Collectors.toList());

        when(mockSessionService.getSessionByAccessToken(accessToken)).thenReturn(sessionItem);
        when(mockAddressService.getAddressItem(sessionId)).thenReturn(addressItem);

        SignedJWTFactory signedJwtFactory = new SignedJWTFactory(new ECDSASigner(getPrivateKey()));
        ConfigurationService mockConfigurationService = mock(ConfigurationService.class);
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("issuer");
        when(mockConfigurationService.getCommonParameterValue(
                        "verifiableCredentialKmsSigningKeyId"))
                .thenReturn(EC_PRIVATE_KEY_1);
        when(mockConfigurationService.getMaxJwtTtl()).thenReturn(10L);
        when(mockConfigurationService.getParameterValue("JwtTtlUnit")).thenReturn("MINUTES");
        ObjectMapper objectMapper = getMapperWithCustomSerializers();

        Clock clock = Clock.fixed(Instant.parse("2099-01-01T00:00:00.00Z"), ZoneId.of("UTC"));
        VerifiableCredentialService verifiableCredentialService =
                new VerifiableCredentialService(
                        signedJwtFactory,
                        mockConfigurationService,
                        objectMapper,
                        new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock));
        handler =
                new IssueCredentialHandler(
                        verifiableCredentialService,
                        mockAddressService,
                        mockSessionService,
                        mockEventProbe,
                        mockAuditService);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockAddressService).getAddressItem(sessionId);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER);
        verify(mockEventProbe).log(INFO, "found session");
        verifyNoMoreInteractions(mockEventProbe);

        Map<String, Object> auditEventExtensions =
                Map.of("iss", "issuer", "addressesEntered", addresses.size(), "isUkAddress", true);
        verify(mockAuditService)
                .sendAuditEvent(
                        eq(AuditEventType.VC_ISSUED),
                        vcEventAuditEventContextArgCaptor.capture(),
                        eq(auditEventExtensions));

        verify(mockAuditService)
                .sendAuditEvent(
                        eq(AuditEventType.END), endEventAuditEventContextArgCaptor.capture());

        AuditEventContext endAuditEventContext = endEventAuditEventContextArgCaptor.getValue();
        AuditEventContext vcAuditEventContext = vcEventAuditEventContextArgCaptor.getValue();

        assertEquals(HttpStatusCode.OK, response.getStatusCode());
        assertEquals(event.getHeaders(), endAuditEventContext.getRequestHeaders());
        assertEquals(sessionItem, endAuditEventContext.getSessionItem());
        assertEquals(sessionItem, endEventAuditEventContextArgCaptor.getValue().getSessionItem());

        assertEquals(
                ContentType.APPLICATION_JWT.getType(), response.getHeaders().get("Content-Type"));

        for (int i = 0; i < addresses.size(); i++) {
            Address addressInAuditContext =
                    vcAuditEventContext.getPersonIdentity().getAddresses().get(i);
            assertEquals(addresses.get(i).getUprn(), addressInAuditContext.getUprn());
            assertEquals(address.getBuildingNumber(), addressInAuditContext.getBuildingNumber());
            assertEquals(address.getStreetName(), addressInAuditContext.getStreetName());
            assertEquals(address.getAddressLocality(), addressInAuditContext.getAddressLocality());
            assertEquals(address.getPostalCode(), addressInAuditContext.getPostalCode());
            assertEquals(address.getAddressCountry(), addressInAuditContext.getAddressCountry());
            assertEquals(address.getAddressRegion(), addressInAuditContext.getAddressRegion());
        }
    }

    @Test
    void shouldThrowJOSEExceptionWhenGenerateVerifiableCredentialIsMalformed()
            throws JOSEException, SqsException, NoSuchAlgorithmException, JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));
        setRequestBodyAsPlainJWT(event);
        when(mockEventProbe.log(INFO, "found session")).thenReturn(mockEventProbe);
        setupEventProbeExpectedErrorBehaviour();
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
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        Map<String, Object> responseBody =
                new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});
        assertThat(
                responseBody.get("error_description").toString(),
                containsString(VERIFIABLE_CREDENTIAL_ERROR.getErrorSummary()));
        verify(mockEventProbe).log(INFO, "found session");
        verifyNoMoreInteractions(mockEventProbe);
    }

    @Test
    void shouldThrowNoSuchAlgorithmExceptionWhenTheWrongKeyAlgorithmIsUsed()
            throws JOSEException, SqsException, NoSuchAlgorithmException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));
        setRequestBodyAsPlainJWT(event);
        when(mockEventProbe.log(INFO, "found session")).thenReturn(mockEventProbe);
        setupEventProbeExpectedErrorBehaviour();
        var noSuchAlgorithmException = new NoSuchAlgorithmException("Incorrect Algorithm");

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
                .thenThrow(noSuchAlgorithmException);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockAddressService).getAddressItem(sessionId);
        verify(mockVerifiableCredentialService)
                .generateSignedVerifiableCredentialJwt(SUBJECT, canonicalAddresses);
        verify(mockEventProbe).log(Level.ERROR, noSuchAlgorithmException);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
        verifyNoMoreInteractions(mockVerifiableCredentialService);
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertThat(response.getBody(), containsString(NO_SUCH_ALGORITHM_ERROR));
        verify(mockEventProbe).log(INFO, "found session");
        verifyNoMoreInteractions(mockEventProbe);
    }

    @Test
    void shouldThrowCredentialRequestExceptionWhenAuthorizationHeaderIsNotSupplied()
            throws SqsException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        setupEventProbeExpectedErrorBehaviour();
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        assertEquals(
                ContentType.APPLICATION_JSON.getType(), response.getHeaders().get("Content-Type"));
        assertEquals(HttpStatusCode.BAD_REQUEST, response.getStatusCode());
        verifyNoMoreInteractions(mockEventProbe);
    }

    @Test
    void shouldThrowAWSExceptionWhenAServerErrorOccursRetrievingASessionItemWithAccessToken()
            throws JsonProcessingException, SqsException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setRequestBodyAsPlainJWT(event);
        setupEventProbeExpectedErrorBehaviour();

        AwsErrorDetails awsErrorDetails =
                AwsErrorDetails.builder()
                        .errorCode("")
                        .sdkHttpResponse(
                                SdkHttpResponse.builder()
                                        .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR)
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
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        Map<String, Object> responseBody =
                new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});
        assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), response.getStatusCode());
        assertThat(
                responseBody.get("error_description").toString(),
                containsString(awsErrorDetails.errorMessage()));
        verifyNoMoreInteractions(mockEventProbe);
    }

    @Test
    void shouldThrowAWSExceptionWhenAServerErrorOccursDuringRetrievingAnAddressItemWithSessionId()
            throws JsonProcessingException, SqsException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setRequestBodyAsPlainJWT(event);
        when(mockEventProbe.log(Level.INFO, "found session")).thenReturn(mockEventProbe);
        setupEventProbeExpectedErrorBehaviour();

        AwsErrorDetails awsErrorDetails =
                AwsErrorDetails.builder()
                        .errorCode("")
                        .sdkHttpResponse(
                                SdkHttpResponse.builder()
                                        .statusCode(HttpStatusCode.INTERNAL_SERVER_ERROR)
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
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        Map<String, Object> responseBody =
                new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});
        assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), response.getStatusCode());
        assertThat(
                responseBody.get("error_description").toString(),
                containsString(awsErrorDetails.errorMessage()));
        verify(mockEventProbe).log(INFO, "found session");
        verifyNoMoreInteractions(mockEventProbe);
    }

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    void shouldThrowAccessDeniedErrorWhenRetrievingASessionItemNotFoundWithAnAccessToken(
            Class<? extends Throwable> exceptionClass)
            throws JsonProcessingException, SqsException {
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        AccessToken accessToken = new BearerAccessToken();
        event.withHeaders(
                Map.of(
                        IssueCredentialHandler.AUTHORIZATION_HEADER_KEY,
                        accessToken.toAuthorizationHeader()));

        setRequestBodyAsPlainJWT(event);
        setupEventProbeExpectedErrorBehaviour();

        when(mockSessionService.getSessionByAccessToken(accessToken)).thenThrow(exceptionClass);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);
        Map<String, Object> responseBody =
                new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {});

        verify(mockSessionService).getSessionByAccessToken(accessToken);
        verify(mockEventProbe).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
        verify(mockEventProbe).log(any(), exceptionCaptor.capture());
        verify(mockAuditService, never()).sendAuditEvent(any(AuditEventType.class));
        verifyNoMoreInteractions(mockEventProbe);

        assertEquals(HttpStatusCode.FORBIDDEN, response.getStatusCode());
        assertThat(
                responseBody.get("error_description").toString(),
                containsString("Access denied by resource owner or authorization server"));
        assertEquals(exceptionClass, exceptionCaptor.getValue().getClass());
    }

    private void setupEventProbeExpectedErrorBehaviour() {
        when(mockEventProbe.log(eq(Level.ERROR), Mockito.any(Exception.class)))
                .thenReturn(mockEventProbe);
        when(mockEventProbe.counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d))
                .thenReturn(mockEventProbe);
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

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(
                Arguments.of(
                        SessionNotFoundException.class, "no session found with that access token"),
                Arguments.of(SessionExpiredException.class, "session expired"),
                Arguments.of(AccessTokenExpiredException.class, "access code expired"));
    }

    private ECPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return (ECPrivateKey)
                KeyFactory.getInstance("EC")
                        .generatePrivate(
                                new PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(EC_PRIVATE_KEY_1)));
    }
}
