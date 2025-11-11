package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.ErrorObject;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.httpclient.JavaHttpClientTelemetry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import uk.gov.di.ipv.cri.address.api.exceptions.ClientIdNotSupportedException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupBadRequestException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupTimeoutException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeValidationException;
import uk.gov.di.ipv.cri.address.api.models.Postcode;
import uk.gov.di.ipv.cri.address.api.service.PostcodeLookupService;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditEventFactory;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.TempCleaner;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.nimbusds.oauth2.sdk.OAuth2Error.ACCESS_DENIED;
import static software.amazon.awssdk.http.HttpStatusCode.BAD_REQUEST;
import static software.amazon.awssdk.http.HttpStatusCode.FORBIDDEN;
import static software.amazon.awssdk.http.HttpStatusCode.NOT_FOUND;
import static software.amazon.awssdk.http.HttpStatusCode.OK;
import static software.amazon.awssdk.http.HttpStatusCode.REQUEST_TIMEOUT;
import static software.amazon.awssdk.http.HttpStatusCode.UNAUTHORIZED;
import static uk.gov.di.ipv.cri.address.library.error.ErrorResponse.LOOKUP_SERVER;
import static uk.gov.di.ipv.cri.address.library.error.ErrorResponse.LOOKUP_TIMEOUT;
import static uk.gov.di.ipv.cri.address.library.error.ErrorResponse.LOOK_ERROR;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.INVALID_POSTCODE;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_EXPIRED;
import static uk.gov.di.ipv.cri.common.library.error.ErrorResponse.SESSION_NOT_FOUND;

@SuppressWarnings("javaarchitecture:S7091")
public class PostcodeLookupHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final PostcodeLookupService postcodeLookupService;
    private final SessionService sessionService;
    private final EventProbe eventProbe;
    private final AuditService auditService;
    protected static final String SESSION_ID = "session_id";
    protected static final String LAMBDA_NAME = "postcode_lookup";
    protected static final String POSTCODE_ERROR = "postcode_lookup_error";
    protected static final String POSTCODE_ERROR_TYPE = "postcode_lookup_error_type";
    protected static final String POSTCODE_ERROR_MESSAGE = "postcode_lookup_error_message";

    public static final long CONNECTION_TIMEOUT_SECONDS = 15;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ExcludeFromGeneratedCoverageReport
    public PostcodeLookupHandler() {
        TempCleaner.clean();

        ClientProviderFactory clientProviderFactory = new ClientProviderFactory();

        ConfigurationService configurationService =
                new ConfigurationService(
                        clientProviderFactory.getSSMProvider(),
                        clientProviderFactory.getSecretsProvider());

        HttpClient httpClient =
                JavaHttpClientTelemetry.builder(GlobalOpenTelemetry.get())
                        .build()
                        .newHttpClient(
                                HttpClient.newBuilder()
                                        .version(HttpClient.Version.HTTP_2)
                                        .connectTimeout(
                                                Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                                        .build());

        this.eventProbe = new EventProbe();

        this.postcodeLookupService =
                new PostcodeLookupService(
                        configurationService,
                        httpClient,
                        LogManager.getLogger(),
                        eventProbe,
                        OBJECT_MAPPER);

        this.sessionService =
                new SessionService(
                        configurationService, clientProviderFactory.getDynamoDbEnhancedClient());

        this.auditService =
                new AuditService(
                        clientProviderFactory.getSqsClient(),
                        configurationService,
                        OBJECT_MAPPER,
                        new AuditEventFactory(configurationService, Clock.systemDefaultZone()));
    }

    public PostcodeLookupHandler(
            PostcodeLookupService postcodeLookupService,
            SessionService sessionService,
            EventProbe eventProbe,
            AuditService auditService) {
        this.postcodeLookupService = postcodeLookupService;
        this.sessionService = sessionService;
        this.eventProbe = eventProbe;
        this.auditService = auditService;
    }

    @Override
    @Logging(correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST, clearState = true)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        String sessionId = input.getHeaders().get(SESSION_ID);

        try {
            String postcode = getPostcodeFromRequest(input);

            SessionItem sessionItem = sessionService.validateSessionId(sessionId);
            eventProbe.log(Level.INFO, "found session");

            auditService.sendAuditEvent(
                    AuditEventType.REQUEST_SENT,
                    postcodeLookupService.getAuditEventContext(
                            postcode, input.getHeaders(), sessionItem));

            List<CanonicalAddress> results =
                    postcodeLookupService.lookupPostcode(postcode, sessionItem.getClientId());

            eventProbe.counterMetric(LAMBDA_NAME);
            auditService.sendAuditEvent(
                    AuditEventType.RESPONSE_RECEIVED,
                    postcodeLookupService.getAuditEventContext(
                            postcode, input.getHeaders(), sessionItem));

            return ApiGatewayResponseGenerator.proxyJsonResponse(OK, results);
        } catch (PostcodeValidationException | PostcodeLookupBadRequestException e) {
            return handleException(e, INVALID_POSTCODE.getMessage(), BAD_REQUEST);
        } catch (PostcodeLookupTimeoutException e) {
            return handleException(e, LOOKUP_TIMEOUT.getMessage(), REQUEST_TIMEOUT);
        } catch (PostcodeLookupProcessingException e) {
            return handleException(e, LOOK_ERROR.getMessage(), NOT_FOUND);
        } catch (SessionExpiredException e) {
            return handleException(e, ACCESS_DENIED, SESSION_EXPIRED.getMessage(), FORBIDDEN);
        } catch (SessionNotFoundException e) {
            return handleException(e, ACCESS_DENIED, SESSION_NOT_FOUND.getMessage(), FORBIDDEN);
        } catch (ClientIdNotSupportedException e) {
            return handleException(e, LOOKUP_SERVER.getMessage(), BAD_REQUEST);
        } catch (Exception e) {
            return handleException(e, LOOKUP_SERVER.getMessage(), UNAUTHORIZED);
        }
    }

    private String getPostcodeFromRequest(APIGatewayProxyRequestEvent input)
            throws PostcodeLookupBadRequestException {
        try {
            Postcode postcode = OBJECT_MAPPER.readValue(input.getBody(), Postcode.class);

            if (postcode != null && postcode.getValue() != null) {
                return postcode.getValue();
            }
            throw new PostcodeLookupBadRequestException("Missing postcode in request body.");
        } catch (JsonProcessingException e) {
            throw new PostcodeLookupBadRequestException(
                    "Failed to parse postcode from request body", e);
        }
    }

    private APIGatewayProxyResponseEvent handleException(
            Exception e, String message, int statusCode) {
        return handleException(e, null, message, statusCode);
    }

    private APIGatewayProxyResponseEvent handleException(
            Exception e, ErrorObject oauth2ErrorType, String message, int statusCode) {
        setPostCodeLookupErrorMetrics(e, message);

        if (Objects.nonNull(oauth2ErrorType)) {
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    oauth2ErrorType.getHTTPStatusCode(),
                    oauth2ErrorType.appendDescription(" - " + message).toJSONObject());
        }
        return ApiGatewayResponseGenerator.proxyJsonResponse(statusCode, e.getMessage());
    }

    private void setPostCodeLookupErrorMetrics(Exception e, String message) {
        String[] formatMessage = message.toLowerCase().split(" ");
        String metricErrorType = Arrays.stream(formatMessage).collect(Collectors.joining("_"));

        eventProbe.log(Level.ERROR, e).counterMetric(POSTCODE_ERROR);
        eventProbe.addDimensions(
                Map.of(
                        POSTCODE_ERROR_TYPE,
                        metricErrorType,
                        POSTCODE_ERROR_MESSAGE,
                        e.getMessage()));
    }
}
