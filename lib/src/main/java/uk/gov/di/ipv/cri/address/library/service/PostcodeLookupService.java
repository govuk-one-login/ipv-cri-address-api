package uk.gov.di.ipv.cri.address.library.service;

import com.google.gson.Gson;
import org.apache.http.client.utils.URIBuilder;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupProcessingException;
import uk.gov.di.ipv.cri.address.library.exception.PostcodeLookupValidationException;
import uk.gov.di.ipv.cri.address.library.models.PostcodeResult;
import uk.gov.di.ipv.cri.address.library.models.ordinancesurvey.OrdinanceSurveyPostcodeResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;

public class PostcodeLookupService {

    // Create our http client to enable asynchronous requests
    private final HttpClient client;

    private final ConfigurationService configurationService;

    public PostcodeLookupService() {
        configurationService = new ConfigurationService();
        client =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
    }

    public PostcodeLookupService(ConfigurationService configurationService, HttpClient client) {
        this.configurationService = configurationService;
        this.client = client;
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

        // If we have a 404, return an empty list
        if (response.statusCode() == 404) {
            return new ArrayList<>();
        }

        // If it's not OK, throw an exception
        if (response.statusCode() != 200) {
            throw new PostcodeLookupProcessingException(
                    "Error processing postcode lookup: " + response.body());
        }

        // Otherwise, let's try to parse the response
        OrdinanceSurveyPostcodeResponse postcodeResponse = new OrdinanceSurveyPostcodeResponse();

        String body = response.body();
        Gson gson = new Gson();
        postcodeResponse = gson.fromJson(body, postcodeResponse.getClass());

        // Map the postcode response to our model
        return postcodeResponse.getResults().stream()
                .map(PostcodeResult::new)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
