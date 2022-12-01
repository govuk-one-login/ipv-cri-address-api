package gov.uk.address.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.client.HttpHeaders;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.util.URIBuilder;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

public class AddressApiClient {
    private final HttpClient httpClient;
    private final ClientConfigurationService clientConfigurationService;
    private final ObjectMapper objectMapper;
    private static final String JSON_MIME_MEDIA_TYPE = "application/json";

    public AddressApiClient(ClientConfigurationService clientConfigurationService) {
        this.clientConfigurationService = clientConfigurationService;
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public HttpResponse<String> sendPostCodeLookUpRequest(String sessionId, String postcode)
            throws IOException, InterruptedException {
        postcode = postcode.trim().replaceAll("\\s", "%20");
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPrivateApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "postcode-lookup/" + postcode))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header("session_id", sessionId)
                        .GET()
                        .build();
        return sendHttpRequest(request);
    }

    public HttpResponse<String> sendAddressRequest(String sessionId, String uprn, String postcode)
            throws IOException, InterruptedException {

        CanonicalAddress currentAddress = new CanonicalAddress();
        currentAddress.setUprn(Long.parseLong(uprn));
        currentAddress.setPostalCode(postcode);
        currentAddress.setValidFrom(LocalDate.of(2020, 1, 1));

        var requestBody = objectMapper.writeValueAsString(new CanonicalAddress[] {currentAddress});

        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPrivateApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "address"))
                                        .build())
                        .header("session_id", sessionId)
                        .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
        return sendHttpRequest(request);
    }

    public HttpResponse<String> sendCredentialIssueRequest(String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPublicApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "/credential/issue"))
                                        .build())
                        .header("x-api-key", this.clientConfigurationService.getPublicApiKey())
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return sendHttpRequest(request);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
