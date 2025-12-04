package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import org.apache.logging.log4j.Level;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.lambda.powertools.logging.CorrelationIdPaths;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.FlushMetrics;
import uk.gov.di.ipv.cri.address.api.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.api.service.VerifiableCredentialService;
import uk.gov.di.ipv.cri.address.library.exception.AddressNotFoundException;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressItem;
import uk.gov.di.ipv.cri.address.library.service.AddressService;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditEventFactory;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityDetailedBuilder;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.KMSSigner;
import uk.gov.di.ipv.cri.common.library.util.SignedJWTFactory;
import uk.gov.di.ipv.cri.common.library.util.TempCleaner;
import uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder;

import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.logging.log4j.Level.ERROR;
import static uk.gov.di.ipv.cri.address.api.objectmapper.CustomObjectMapper.getMapperWithCustomSerializers;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.ACCESS_TOKEN_EXPIRED;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_EXPIRED;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_NOT_FOUND;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.VERIFIABLE_CREDENTIAL_ERROR;

public class IssueCredentialHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    public static final String ADDRESS_CREDENTIAL_ISSUER = "address_credential_issuer";
    public static final String NO_SUCH_ALGORITHM_ERROR =
            "The algorithm name provided is incorrect or misspelled, should be ES256.";
    public static final String ADDRESS_NOT_FOUND_TEMPLATE = " - %d: %s";
    public static final int ADDR_NOT_FOUND_ERR_CODE = 2008;
    private final VerifiableCredentialService verifiableCredentialService;
    private final AddressService addressService;
    private final SessionService sessionService;
    private final EventProbe eventProbe;
    private final AuditService auditService;

    public IssueCredentialHandler(
            VerifiableCredentialService verifiableCredentialService,
            AddressService addressService,
            SessionService sessionService,
            EventProbe eventProbe,
            AuditService auditService) {
        this.verifiableCredentialService = verifiableCredentialService;
        this.addressService = addressService;
        this.sessionService = sessionService;
        this.eventProbe = eventProbe;
        this.auditService = auditService;
    }

    @ExcludeFromGeneratedCoverageReport
    public IssueCredentialHandler() {
        TempCleaner.clean();

        ClientProviderFactory clientProviderFactory = new ClientProviderFactory();

        ConfigurationService config =
                new ConfigurationService(
                        clientProviderFactory.getSSMProvider(),
                        clientProviderFactory.getSecretsProvider());

        ObjectMapper objectMapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule());

        String kmsSigningKeyId = config.getVerifiableCredentialKmsSigningKeyId();

        SignedJWTFactory signedJWTFactory =
                new SignedJWTFactory(
                        new KMSSigner(kmsSigningKeyId, clientProviderFactory.getKMSClient()));

        this.verifiableCredentialService =
                new VerifiableCredentialService(
                        signedJWTFactory,
                        config,
                        getMapperWithCustomSerializers(),
                        new VerifiableCredentialClaimsSetBuilder(config, Clock.systemUTC()));

        this.addressService =
                new AddressService(objectMapper, clientProviderFactory.getDynamoDbEnhancedClient());
        this.sessionService =
                new SessionService(config, clientProviderFactory.getDynamoDbEnhancedClient());
        this.eventProbe = new EventProbe();
        this.auditService =
                new AuditService(
                        clientProviderFactory.getSqsClient(),
                        config,
                        objectMapper,
                        new AuditEventFactory(config, Clock.systemUTC()));
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPaths.API_GATEWAY_REST, clearState = true)
    @FlushMetrics(namespace = "di-ipv-cri-address-api", captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        try {
            var accessToken = validateInputHeaderBearerToken(input.getHeaders());
            var sessionItem = this.sessionService.getSessionByAccessToken(accessToken);

            eventProbe.log(Level.INFO, "found session");

            AddressItem addressItem = this.addressService.getAddressItemWithRetries(sessionItem);

            SignedJWT signedJWT =
                    verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                            sessionItem.getSubject(), addressItem.getAddresses());

            sendVcIssuedAuditEvent(input.getHeaders(), addressItem, sessionItem);
            eventProbe.counterMetric(ADDRESS_CREDENTIAL_ISSUER);
            sendEndAuditEvent(input.getHeaders(), sessionItem);

            return ApiGatewayResponseGenerator.proxyJwtResponse(
                    HttpStatusCode.OK, signedJWT.serialize());
        } catch (AwsServiceException ex) {
            eventProbe.log(ERROR, ex).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.SERVER_ERROR.getHTTPStatusCode(),
                    OAuth2Error.SERVER_ERROR
                            .appendDescription(" - " + ex.awsErrorDetails().errorMessage())
                            .toJSONObject());
        } catch (CredentialRequestException | ParseException | JOSEException e) {
            eventProbe.log(ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.INVALID_REQUEST.getHTTPStatusCode(),
                    OAuth2Error.INVALID_REQUEST
                            .appendDescription(
                                    " - " + VERIFIABLE_CREDENTIAL_ERROR.getErrorSummary())
                            .toJSONObject());
        } catch (SqsException sqsException) {
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.SERVER_ERROR.getHTTPStatusCode(),
                    OAuth2Error.SERVER_ERROR
                            .appendDescription(" - " + sqsException.getMessage())
                            .toJSONObject());
        } catch (AccessTokenExpiredException e) {
            eventProbe.log(ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.ACCESS_DENIED.getHTTPStatusCode(),
                    OAuth2Error.ACCESS_DENIED
                            .appendDescription(" - " + ACCESS_TOKEN_EXPIRED.getErrorSummary())
                            .toJSONObject());
        } catch (SessionExpiredException e) {
            eventProbe.log(ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.ACCESS_DENIED.getHTTPStatusCode(),
                    OAuth2Error.ACCESS_DENIED
                            .appendDescription(" - " + SESSION_EXPIRED.getErrorSummary())
                            .toJSONObject());
        } catch (SessionNotFoundException e) {
            eventProbe.log(ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.ACCESS_DENIED.getHTTPStatusCode(),
                    OAuth2Error.ACCESS_DENIED
                            .appendDescription(" - " + SESSION_NOT_FOUND.getErrorSummary())
                            .toJSONObject());
        } catch (AddressNotFoundException e) {
            eventProbe.log(ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    OAuth2Error.ACCESS_DENIED.getHTTPStatusCode(),
                    OAuth2Error.ACCESS_DENIED
                            .appendDescription(
                                    format(
                                            ADDRESS_NOT_FOUND_TEMPLATE,
                                            ADDR_NOT_FOUND_ERR_CODE,
                                            e.getMessage()))
                            .toJSONObject());
        } catch (NoSuchAlgorithmException e) {
            eventProbe.log(ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatusCode.INTERNAL_SERVER_ERROR, NO_SUCH_ALGORITHM_ERROR);
        }
    }

    private void sendEndAuditEvent(Map<String, String> headers, SessionItem sessionItem)
            throws SqsException {
        auditService.sendAuditEvent(
                AuditEventType.END, new AuditEventContext(headers, sessionItem));
    }

    private void sendVcIssuedAuditEvent(
            Map<String, String> headers, AddressItem addressItem, SessionItem sessionItem)
            throws SqsException {
        AuditEventContext auditEventContext =
                new AuditEventContext(
                        PersonIdentityDetailedBuilder.builder()
                                .withAddresses(
                                        addressItem.getAddresses().stream()
                                                .map(Address::new)
                                                .collect(Collectors.toList()))
                                .build(),
                        headers,
                        sessionItem);

        auditService.sendAuditEvent(
                AuditEventType.VC_ISSUED,
                auditEventContext,
                verifiableCredentialService.getAuditEventExtensions(addressItem.getAddresses()));
    }

    private AccessToken validateInputHeaderBearerToken(Map<String, String> headers)
            throws CredentialRequestException, ParseException {
        var token =
                Optional.ofNullable(headers).stream()
                        .flatMap(x -> x.entrySet().stream())
                        .filter(
                                header ->
                                        AUTHORIZATION_HEADER_KEY.equalsIgnoreCase(header.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new CredentialRequestException(
                                                ErrorResponse.MISSING_AUTHORIZATION_HEADER));

        return AccessToken.parse(token, AccessTokenType.BEARER);
    }
}
