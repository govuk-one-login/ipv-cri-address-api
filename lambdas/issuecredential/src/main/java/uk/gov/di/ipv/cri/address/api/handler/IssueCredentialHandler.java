package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.api.exception.CredentialRequestException;
import uk.gov.di.ipv.cri.address.api.service.VerifiableCredentialService;
import uk.gov.di.ipv.cri.address.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.address.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.address.library.error.ErrorResponse;
import uk.gov.di.ipv.cri.address.library.exception.SqsException;
import uk.gov.di.ipv.cri.address.library.service.AddressService;
import uk.gov.di.ipv.cri.address.library.service.AuditService;
import uk.gov.di.ipv.cri.address.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.address.library.service.SessionService;
import uk.gov.di.ipv.cri.address.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.address.library.util.EventProbe;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.apache.logging.log4j.Level.ERROR;

public class IssueCredentialHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    public static final String ADDRESS_CREDENTIAL_ISSUER = "address_credential_issuer";
    private final VerifiableCredentialService verifiableCredentialService;
    private final AddressService addressService;
    private final SessionService sessionService;
    private EventProbe eventProbe;
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
        this.verifiableCredentialService = getVerifiableCredentialService();
        this.addressService = new AddressService();
        this.sessionService = new SessionService();
        this.eventProbe = new EventProbe();
        this.auditService =
                new AuditService(
                        AmazonSQSClientBuilder.defaultClient(), new ConfigurationService());
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {

        try {
            var accessToken = validateInputHeaderBearerToken(input.getHeaders());
            var sessionItem = this.sessionService.getSessionByAccessToken(accessToken);
            var addressItem = addressService.getAddresses(sessionItem.getSessionId());

            SignedJWT signedJWT =
                    verifiableCredentialService.generateSignedVerifiableCredentialJwt(
                            sessionItem.getSubject(), addressItem.getAddresses());

            auditService.sendAuditEvent(AuditEventTypes.IPV_ADDRESS_CRI_VC_ISSUED);
            eventProbe.counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJwtResponse(
                    HttpStatus.SC_OK, signedJWT.serialize());
        } catch (AwsServiceException ex) {
            eventProbe.log(ERROR, ex).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, ex.awsErrorDetails().errorMessage());
        } catch (CredentialRequestException | ParseException | JOSEException e) {
            eventProbe.log(ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);

            return ApiGatewayResponseGenerator.proxyJsonResponse(HttpStatus.SC_BAD_REQUEST, 0);
        } catch (SqsException e) {
            eventProbe.log(Level.ERROR, e).counterMetric(ADDRESS_CREDENTIAL_ISSUER, 0d);
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
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

    private VerifiableCredentialService getVerifiableCredentialService() {
        Supplier<VerifiableCredentialService> factory = VerifiableCredentialService::new;
        return factory.get();
    }
}
