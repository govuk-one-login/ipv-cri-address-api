package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import org.apache.http.client.utils.URIBuilder;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupValidationException;
import uk.gov.di.ipv.cri.address.library.models.PostcodeResult;
import uk.gov.di.ipv.cri.address.library.models.ordinancesurvey.OrdinanceSurveyPostcodeError;
import uk.gov.di.ipv.cri.address.library.models.ordinancesurvey.OrdinanceSurveyPostcodeResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class PostcodeLookupService {

    // Create our http client to enable asynchronous requests
    private final HttpClient client;

    private final ConfigurationService configurationService;

    private LambdaLogger logger;

    public PostcodeLookupService() {
        configurationService = new ConfigurationService();
        client =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
        logger =
                new LambdaLogger() {
                    @Override
                    public void log(String message) {
                        System.out.println(message);
                    }

                    @Override
                    public void log(byte[] message) {
                        System.out.println(new String(message));
                    }
                };
    }

    public void setLogger(LambdaLogger logger) {
        this.logger = logger;
    }

    public PostcodeLookupService(
            ConfigurationService configurationService, HttpClient client, LambdaLogger logger) {
        this.configurationService = configurationService;
        this.client = client;
        this.logger = logger;
    }

    public ArrayList<PostcodeResult> lookupPostcode(String postcode)
            throws PostcodeLookupValidationException, PostcodeLookupProcessingException {

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
                                    new URIBuilder(configurationService.getOsApiUrl())
                                            .addParameter("postcode", postcode)
                                            .addParameter("key", configurationService.getOsApiKey())
                                            .build())
                            .header("Accept", "application/json")
                            .GET()
                            .build();
        } catch (URISyntaxException e) {
            throw new PostcodeLookupProcessingException(
                    "Error building URI for postcode lookup", e);
        }

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new PostcodeLookupProcessingException(
                    "Error sending request for postcode lookup", e);
        }

        OrdinanceSurveyPostcodeError error;
        switch (response.statusCode()) {
            case 200:
            case 201:
                // These responses are fine
                break;
            case 400:
                try {
                    error =
                            new Gson()
                                    .fromJson(response.body(), OrdinanceSurveyPostcodeError.class);
                    logger.log(
                            "Ordnance Survey Responded with "
                                    + error.getError().getStatuscode()
                                    + ": "
                                    + error.getError().getMessage());
                } catch (Exception e) {
                    logger.log("Ordnance Survey Responded with unknown error: " + response.body());
                }
                return new ArrayList<>();

            case 404:
                logger.log("Ordnance Survey Responded with 404: Not Found");
                return new ArrayList<>();

            default:
                try {
                    error =
                            new Gson()
                                    .fromJson(response.body(), OrdinanceSurveyPostcodeError.class);
                    logger.log(
                            "Ordnance Survey Responded with "
                                    + error.getError().getStatuscode()
                                    + ": "
                                    + error.getError().getMessage());
                    throw new PostcodeLookupProcessingException(
                            "Ordnance Survey Responded with "
                                    + error.getError().getStatuscode()
                                    + ": "
                                    + error.getError().getMessage());
                } catch (Exception e) {
                    logger.log("Ordnance Survey Responded with unknown error: " + response.body());
                }
                throw new PostcodeLookupProcessingException(
                        "Error processing postcode lookup: " + response.body());
        }

        // Otherwise, let's try to parse the response
        OrdinanceSurveyPostcodeResponse postcodeResponse = new OrdinanceSurveyPostcodeResponse();

        postcodeResponse = new Gson().fromJson(response.body(), postcodeResponse.getClass());

        // Map the postcode response to our model
        return (ArrayList<PostcodeResult>)
                postcodeResponse.getResults().stream()
                        .map(PostcodeResult::new)
                        .collect(Collectors.toList());
    }
}
