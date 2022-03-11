package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupValidationException;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddress;
import uk.gov.di.ipv.cri.address.library.models.ordnancesurvey.OrdnanceSurveyPostcodeError;
import uk.gov.di.ipv.cri.address.library.models.ordnancesurvey.OrdnanceSurveyPostcodeResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.di.ipv.cri.address.library.constants.OrdnanceSurveyConstants.LOG_RESPONSE_PREFIX;

public class PostcodeLookupService {

    // Create our http client to enable asynchronous requests
    private final HttpClient client;

    private final ConfigurationService configurationService;

    Logger log = LogManager.getLogger();

    public PostcodeLookupService() {
        configurationService = new ConfigurationService();
        client =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
    }

    public PostcodeLookupService(
            ConfigurationService configurationService, HttpClient client, Logger log) {
        this.configurationService = configurationService;
        this.client = client;
        this.log = log;
    }

    public List<CanonicalAddress> lookupPostcode(String postcode)
            throws PostcodeLookupValidationException, PostcodeLookupProcessingException,
                    JsonProcessingException {

        // Check the postcode is valid
        if (postcode == null || postcode.isEmpty()) {
            throw new PostcodeLookupValidationException("Postcode cannot be null or empty");
        }

        // Create our http request
        HttpRequest request;
        HttpResponse<String> response;

        try {
            request =
                    HttpRequest.newBuilder()
                            .uri(
                                    new URIBuilder(configurationService.getOsPostcodeAPIUrl())
                                            .addParameter("postcode", postcode)
                                            .addParameter("key", configurationService.getOsApiKey())
                                            .build())
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
        } catch (IOException e) {
            log.error("Postcode lookup threw an IO exception", e);

            throw new PostcodeLookupProcessingException(
                    "Error sending request for postcode lookup", e);
        }

        OrdnanceSurveyPostcodeError error;
        switch (response.statusCode()) {
            case HttpStatus.SC_OK:
                // These responses are fine
                break;
            case HttpStatus.SC_BAD_REQUEST:
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

            case HttpStatus.SC_NOT_FOUND:
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
        return postcodeResponse.getResults().stream()
                .map(CanonicalAddress::new)
                .collect(Collectors.toList());
    }
}
