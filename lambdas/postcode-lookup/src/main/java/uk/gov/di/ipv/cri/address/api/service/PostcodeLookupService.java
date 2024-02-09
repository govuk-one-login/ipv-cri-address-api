package uk.gov.di.ipv.cri.address.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupTimeoutException;
import uk.gov.di.ipv.cri.address.api.exceptions.PostcodeLookupValidationException;
import uk.gov.di.ipv.cri.address.api.models.Dpa;
import uk.gov.di.ipv.cri.address.api.models.OrdnanceSurveyPostcodeError;
import uk.gov.di.ipv.cri.address.api.models.OrdnanceSurveyPostcodeResponse;
import uk.gov.di.ipv.cri.address.api.models.Result;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.di.ipv.cri.address.api.constants.OrdnanceSurveyConstants.LOG_RESPONSE_PREFIX;

public class PostcodeLookupService {

    // Create our http client to enable asynchronous requests
    private final HttpClient client;

    private final ConfigurationService configurationService;

    long connectionTimeoutSeconds = 10;

    Logger log = LogManager.getLogger();

    public PostcodeLookupService() {
        this.configurationService = new ConfigurationService();
        this.client =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
                        .build();
    }

    public PostcodeLookupService(
            ConfigurationService configurationService, HttpClient client, Logger log) {
        this.configurationService = configurationService;
        this.client = client;
        this.log = log;
    }

    @Tracing
    public List<CanonicalAddress> lookupPostcode(String postcode)
            throws PostcodeLookupValidationException, PostcodeLookupProcessingException,
                    JsonProcessingException {

        // Check the postcode is valid
        if (StringUtils.isBlank(postcode)) {
            throw new PostcodeLookupValidationException("Postcode cannot be null or empty");
        }

        // Create our http request
        HttpRequest request;
        HttpResponse<String> response;

        try {
            request =
                    HttpRequest.newBuilder()
                            .uri(
                                    SdkHttpFullRequest.builder()
                                            .uri(
                                                    new URI(
                                                            configurationService.getParameterValue(
                                                                    "OrdnanceSurveyAPIURL")))
                                            .appendRawQueryParameter(
                                                    "postcode",
                                                    URLDecoder.decode(
                                                            postcode, Charset.defaultCharset()))
                                            .appendRawQueryParameter(
                                                    "key",
                                                    configurationService.getSecretValue(
                                                            "OrdnanceSurveyAPIKey"))
                                            .method(SdkHttpMethod.GET)
                                            .build()
                                            .getUri())
                            .timeout(Duration.ofSeconds(connectionTimeoutSeconds))
                            .header("Accept", "application/json")
                            .GET()
                            .build();
        } catch (URISyntaxException e) {
            log.error("Error creating URI for OS postcode lookup", e);
            throw new PostcodeLookupProcessingException(
                    "Error building URI for postcode lookup", e);
        }

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            log.error("Postcode lookup threw interrupted exception", e);

            // Unblock the thread
            Thread.currentThread().interrupt();
            // Now throw our prettier exception
            throw new PostcodeLookupProcessingException(
                    "Error sending request for postcode lookup", e);
        } catch (HttpConnectTimeoutException e) {
            log.error("Postcode lookup threw HTTP connection timeout exception", e);

            throw new PostcodeLookupTimeoutException(
                    "Error timed out waiting for postcode lookup response", e);
        } catch (IOException e) {
            log.error("Postcode lookup threw an IO exception", e);

            throw new PostcodeLookupProcessingException(
                    "Error sending request for postcode lookup", e);
        }

        OrdnanceSurveyPostcodeError error;
        switch (response.statusCode()) {
            case HttpStatusCode.OK:
                // These responses are fine
                break;
            case HttpStatusCode.BAD_REQUEST:
                try {
                    error =
                            new ObjectMapper()
                                    .readValue(response.body(), OrdnanceSurveyPostcodeError.class);
                    log.error(
                            "{} status {}: {}",
                            LOG_RESPONSE_PREFIX,
                            error.getError().getStatuscode(),
                            error.getError().getMessage());
                } catch (Exception e) {
                    log.error("{} unknown error: {}", LOG_RESPONSE_PREFIX, response.body());
                }
                return new ArrayList<>();

            case HttpStatusCode.NOT_FOUND:
                log.error("{}404: Not Found", LOG_RESPONSE_PREFIX);
                return new ArrayList<>();

            default:
                try {
                    error =
                            new ObjectMapper()
                                    .readValue(response.body(), OrdnanceSurveyPostcodeError.class);
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
                    log.error("{}unknown error: {}", LOG_RESPONSE_PREFIX, response.body());
                }
                throw new PostcodeLookupProcessingException(
                        "Error processing postcode lookup: " + response.body());
        }

        // Otherwise, let's try to parse the response
        OrdnanceSurveyPostcodeResponse postcodeResponse = new OrdnanceSurveyPostcodeResponse();

        postcodeResponse =
                new ObjectMapper().readValue(response.body(), postcodeResponse.getClass());

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

    public AuditEventContext getAuditEventContext(
            String postcode, Map<String, String> requestHeaders, SessionItem sessionItem) {
        Objects.requireNonNull(requestHeaders, "requestHeaders must not be null");
        Objects.requireNonNull(sessionItem, "sessionItem must not be null");

        if (StringUtils.isBlank(postcode)) {
            throw new IllegalArgumentException("postcode must not be null or blank");
        }
        Address address = new Address();
        String beautifiedPostcode =
                URLDecoder.decode(postcode, Charset.defaultCharset()).toUpperCase();
        address.setPostalCode(beautifiedPostcode);

        return new AuditEventContext(
                new PersonIdentityDetailed(null, null, List.of(address)),
                requestHeaders,
                sessionItem);
    }
}
