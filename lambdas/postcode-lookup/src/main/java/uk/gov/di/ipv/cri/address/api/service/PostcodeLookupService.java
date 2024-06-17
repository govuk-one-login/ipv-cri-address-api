package uk.gov.di.ipv.cri.address.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupBadRequestException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupTimeoutException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeValidationException;
import uk.gov.di.ipv.cri.address.api.models.Dpa;
import uk.gov.di.ipv.cri.address.api.models.OrdnanceSurveyPostcodeError;
import uk.gov.di.ipv.cri.address.api.models.OrdnanceSurveyPostcodeResponse;
import uk.gov.di.ipv.cri.address.api.models.Result;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.PersonIdentityDetailedBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.di.ipv.cri.address.api.constants.OrdnanceSurveyConstants.LOG_RESPONSE_PREFIX;

public class PostcodeLookupService {
    private static final String POSTCODE_LOOKUP_NO_SUCH_FIELD_ERROR =
            "Postcode lookup threw a NoSuchFieldError. "
                    + "This likely indicates a library version mismatch. "
                    + "Ensure all dependencies are compatible and properly versioned.";
    private static final String POSTCODE_LOOKUP_NO_SUCH_FIELD_ERROR_MESSAGE =
            "Error occurred due to library incompatibility issues. "
                    + "A field was not found, indicating a potential version mismatch "
                    + "in your dependencies. Check your build configuration.";
    // Create our http client to enable asynchronous requests
    private final HttpClient client;
    private final Logger log;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConfigurationService configurationService;
    private static final long CONNECTION_TIMEOUT_SECONDS = 15;

    @ExcludeFromGeneratedCoverageReport
    public PostcodeLookupService() {
        this(new ConfigurationService(), getHttpClient(), LogManager.getLogger());
    }

    public PostcodeLookupService(
            ConfigurationService configurationService, HttpClient client, Logger log) {
        this.configurationService = configurationService;
        this.client = client;
        this.log = log;
    }

    @Tracing
    public List<CanonicalAddress> lookupPostcode(String postcode)
            throws PostcodeValidationException, PostcodeLookupProcessingException,
                    JsonProcessingException, PostcodeLookupBadRequestException {

        this.validatePostCode(postcode);
        // Create our http request
        HttpRequest request = createHttpRequest(postcode);
        HttpResponse<String> response = sendHttpRequest(request);

        switch (response.statusCode()) {
            case HttpStatusCode.OK:
                return processOrdnanceSurveySuccessResponse(response.body());
            case HttpStatusCode.BAD_REQUEST:
                return processOrdnanceSurveyBadResponse(response.body());
            case HttpStatusCode.NOT_FOUND:
                log.error("{}404: Not Found", LOG_RESPONSE_PREFIX);
                return Collections.emptyList();
            default:
                return processOrdnanceSurveyErrorResponse(response.body());
        }
    }

    public AuditEventContext getAuditEventContext(
            String postcode, Map<String, String> requestHeaders, SessionItem sessionItem) {
        Objects.requireNonNull(requestHeaders, "requestHeaders must not be null");
        Objects.requireNonNull(sessionItem, "sessionItem must not be null");
        this.validatePostCode(postcode);

        Address address = new Address();
        address.setPostalCode(URLDecoder.decode(postcode, Charset.defaultCharset()).toUpperCase());

        return new AuditEventContext(
                PersonIdentityDetailedBuilder.builder().withAddresses(List.of(address)).build(),
                requestHeaders,
                sessionItem);
    }

    private static HttpClient getHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .build();
    }

    private HttpRequest createHttpRequest(String postcode)
            throws PostcodeLookupBadRequestException {
        try {
            String urlParam = configurationService.getParameterValue("OrdnanceSurveyAPIURL");
            String apiKey = configurationService.getSecretValue("OrdnanceSurveyAPIKey");
            URI ordnanceSurveyAPIURL = new URI(urlParam);

            return HttpRequest.newBuilder()
                    .uri(
                            SdkHttpFullRequest.builder()
                                    .uri(ordnanceSurveyAPIURL)
                                    .appendRawQueryParameter(
                                            "postcode",
                                            URLDecoder.decode(postcode, Charset.defaultCharset()))
                                    .appendRawQueryParameter("key", apiKey)
                                    .method(SdkHttpMethod.GET)
                                    .build()
                                    .getUri())
                    .timeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
        } catch (URISyntaxException e) {
            log.error("Error creating URI for OS postcode lookup", e);

            throw new PostcodeLookupBadRequestException(
                    "Error building URI for postcode lookup", e);
        }
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException | SocketTimeoutException e) {
            log.error("Postcode lookup threw HTTP connection timeout exception", e);
            throw new PostcodeLookupTimeoutException(
                    "Error timed out waiting for postcode lookup response", e);
        } catch (InterruptedException e) {
            log.error("Postcode lookup threw interrupted exception", e);
            // Unblock the thread
            Thread.currentThread().interrupt();
            // Now throw our prettier exception
            throw new PostcodeLookupProcessingException(
                    "Error sending request for postcode lookup - Interrupted exception", e);
        } catch (NoSuchFieldError e) {
            log.error(POSTCODE_LOOKUP_NO_SUCH_FIELD_ERROR, e);
            throw new PostcodeLookupProcessingException(
                    POSTCODE_LOOKUP_NO_SUCH_FIELD_ERROR_MESSAGE, e);
        } catch (IOException e) {
            log.error("Postcode lookup threw an IO exception", e);
            throw new PostcodeLookupProcessingException(
                    "Error sending request for postcode lookup - IO exception", e);
        }
    }

    private List<CanonicalAddress> processOrdnanceSurveyErrorResponse(String response) {
        try {
            OrdnanceSurveyPostcodeError error =
                    objectMapper.readValue(response, OrdnanceSurveyPostcodeError.class);
            log.error(
                    "{} status {}: {}",
                    LOG_RESPONSE_PREFIX,
                    error.getError().getStatuscode(),
                    error.getError().getMessage());
            throw new PostcodeLookupProcessingException(
                    LOG_RESPONSE_PREFIX
                            + error.getError().getStatuscode()
                            + ": "
                            + error.getError().getMessage());
        } catch (Exception e) {
            log.error("{}unknown error: {}", LOG_RESPONSE_PREFIX, response);
            throw new PostcodeLookupProcessingException(
                    "Error processing postcode lookup: " + response);
        }
    }

    private List<CanonicalAddress> processOrdnanceSurveyBadResponse(String response) {
        try {
            OrdnanceSurveyPostcodeError error =
                    objectMapper.readValue(response, OrdnanceSurveyPostcodeError.class);
            log.error(
                    "{} status {}: {}",
                    LOG_RESPONSE_PREFIX,
                    error.getError().getStatuscode(),
                    error.getError().getMessage());
        } catch (Exception e) {
            log.error("{} unknown error: {}", LOG_RESPONSE_PREFIX, response);
        }
        return Collections.emptyList();
    }

    private List<CanonicalAddress> processOrdnanceSurveySuccessResponse(String response)
            throws JsonProcessingException {
        // These responses are fine
        // Otherwise, let's try to parse the response
        OrdnanceSurveyPostcodeResponse postcodeResponse =
                objectMapper.readValue(response, OrdnanceSurveyPostcodeResponse.class);
        // Map the postcode response to our model
        return Optional.ofNullable(postcodeResponse.getResults())
                .map(
                        results ->
                                results.stream()
                                        .map(Result::getDpa)
                                        .filter(Objects::nonNull)
                                        .map(Dpa::toCanonicalAddress)
                                        .collect(Collectors.toList()))
                .orElseGet(
                        () -> {
                            log.warn("PostCode lookup returned no results");
                            return Collections.emptyList();
                        });
    }

    private void validatePostCode(String postcode) {
        if (isBlank(postcode)) {
            throw new PostcodeValidationException("Postcode must not be null or blank");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
