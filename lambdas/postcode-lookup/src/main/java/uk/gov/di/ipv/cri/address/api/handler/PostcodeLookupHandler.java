package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.nimbusds.oauth2.sdk.ErrorObject;
import org.apache.logging.log4j.Level;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupBadRequestException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupTimeoutException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeValidationException;
import uk.gov.di.ipv.cri.address.api.service.PostcodeLookupService;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.AuditService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;
import uk.gov.di.ipv.cri.common.library.util.ApiGatewayResponseGenerator;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

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

public class PostcodeLookupHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final PostcodeLookupService postcodeLookupService;
    private final SessionService sessionService;
    private final EventProbe eventProbe;
    private final AuditService auditService;
    protected static final String SESSION_ID = "session_id";
    protected static final String LAMBDA_NAME = "postcode_lookup";
    protected static final String POSTCODE_ERROR = "postcode_lookup_error";

    @ExcludeFromGeneratedCoverageReport
    public PostcodeLookupHandler() {
        this(
                new PostcodeLookupService(),
                new SessionService(),
                new EventProbe(),
                new AuditService());
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
    @Tracing
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        String sessionId = input.getHeaders().get(SESSION_ID);
        String postcode = input.getPathParameters().get("postcode");

        try {
            SessionItem sessionItem = sessionService.validateSessionId(sessionId);
            eventProbe.log(Level.INFO, "found session");
            auditService.sendAuditEvent(
                    AuditEventType.REQUEST_SENT,
                    postcodeLookupService.getAuditEventContext(
                            postcode, input.getHeaders(), sessionItem));

            List<CanonicalAddress> results = postcodeLookupService.lookupPostcode(postcode);

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
        } catch (Exception e) {
            return handleException(e, LOOKUP_SERVER.getMessage(), UNAUTHORIZED);
        }
    }

    private APIGatewayProxyResponseEvent handleException(
            Exception e, String message, int statusCode) {
        return handleException(e, null, message, statusCode);
    }

    private APIGatewayProxyResponseEvent handleException(
            Exception e, ErrorObject oauth2ErrorType, String message, int statusCode) {
        String[] formatMessage = message.toLowerCase().split(" ");
        String metricName = Arrays.stream(formatMessage).collect(Collectors.joining("_"));

        eventProbe.log(Level.ERROR, e).counterMetric(POSTCODE_ERROR);
        eventProbe.addDimensions(Map.of(metricName, e.getMessage()));

        if (Objects.nonNull(oauth2ErrorType)) {
            return ApiGatewayResponseGenerator.proxyJsonResponse(
                    oauth2ErrorType.getHTTPStatusCode(),
                    oauth2ErrorType.appendDescription(" - " + message).toJSONObject());
        }
        return ApiGatewayResponseGenerator.proxyJsonResponse(statusCode, e.getMessage());
    }
}
