package gov.uk.address.api.client;

import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.client.HttpHeaders;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AddressApiClient {
    private final HttpClient httpClient;
    private final ClientConfigurationService clientConfigurationService;
    private static final String JSON_MIME_MEDIA_TYPE = "application/json";

    public AddressApiClient(ClientConfigurationService clientConfigurationService) {
        this.clientConfigurationService = clientConfigurationService;
        this.httpClient = HttpClient.newBuilder().build();
    }

    public HttpResponse<String> sendPostCodeLookUpRequest(String sessionId, String postcode)
            throws IOException, InterruptedException {
        postcode = postcode.trim().replaceAll("\\s", "%20");
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new uk.gov.di.ipv.cri.common.library.util.URIBuilder(
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

        var requestBody =
                "[\n"
                        + "  {\n"
                        + "    \"uprn\": "
                        + uprn
                        + ",\n"
                        + "    \"postalCode\": \""
                        + postcode
                        + "\",\n"
                        + "    \"validFrom\": \"2020-01-01\",\n"
                        + "    \"validUntil\": \"\"\n"
                        + "  }\n"
                        + "]";

        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new uk.gov.di.ipv.cri.common.library.util.URIBuilder(
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
                                new uk.gov.di.ipv.cri.common.library.util.URIBuilder(
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
