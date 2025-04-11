package gov.uk.address.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.uk.address.api.util.AddressContext;
import gov.uk.address.api.util.AddressContextMapper;
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
    public static final String SESSION_ID = "session_id";
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

        String privateApiEndpoint = this.clientConfigurationService.getPrivateApiEndpoint();
        return sendHttpRequest(
                requestBuilder(privateApiEndpoint, "postcode-lookup")
                        .header(SESSION_ID, sessionId)
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        "{\"postcode\": \"" + postcode + "\"}"))
                        .build());
    }

    public HttpResponse<String> sendPostCodeLookUpGETRequest(String sessionId, String postcode)
            throws IOException, InterruptedException {

        String privateApiEndpoint = this.clientConfigurationService.getPrivateApiEndpoint();
        return sendHttpRequest(
                requestBuilder(privateApiEndpoint, "postcode-lookup/" + postcode)
                        .header(SESSION_ID, sessionId)
                        .GET()
                        .build());
    }

    public HttpResponse<String> sendAddressRequest(
            String sessionId, String uprn, String postcode, String countryCode)
            throws IOException, InterruptedException {

        CanonicalAddress currentAddress = new CanonicalAddress();
        currentAddress.setUprn(Long.parseLong(uprn));
        currentAddress.setPostalCode(postcode);
        currentAddress.setValidFrom(LocalDate.of(2020, 1, 1));
        currentAddress.setAddressCountry(countryCode);

        String requestBody =
                objectMapper.writeValueAsString(new CanonicalAddress[] {currentAddress});

        String privateApiEndpoint = this.clientConfigurationService.getPrivateApiEndpoint();
        return sendHttpRequest(
                requestBuilder(privateApiEndpoint, "address")
                        .header(SESSION_ID, sessionId)
                        .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build());
    }

    public HttpResponse<String> sendPreviousAddressRequest(
            String sessionId, String uprn, String postcode, String countryCode)
            throws IOException, InterruptedException {

        CanonicalAddress previousAddress = new CanonicalAddress();
        previousAddress.setUprn(Long.parseLong(uprn));
        previousAddress.setPostalCode(postcode);
        previousAddress.setAddressCountry(countryCode);

        CanonicalAddress newAddress = new CanonicalAddress();
        newAddress.setUprn(Long.parseLong(uprn));
        newAddress.setPostalCode(postcode);
        newAddress.setValidFrom(LocalDate.now().minusMonths(3));
        newAddress.setAddressCountry(countryCode);

        String previousAddressRequestBody =
                objectMapper.writeValueAsString(
                        new CanonicalAddress[] {previousAddress, newAddress});

        String privateApiEndpoint = this.clientConfigurationService.getPrivateApiEndpoint();
        return sendHttpRequest(
                requestBuilder(privateApiEndpoint, "address")
                        .header(SESSION_ID, sessionId)
                        .PUT(HttpRequest.BodyPublishers.ofString(previousAddressRequestBody))
                        .build());
    }

    public HttpResponse<String> sendAddressRequest(String sessionId, AddressContext addressContext)
            throws IOException, InterruptedException {

        CanonicalAddress currentAddress =
                AddressContextMapper.mapToCanonicalAddress(addressContext);

        String requestBody =
                objectMapper.writeValueAsString(new CanonicalAddress[] {currentAddress});

        String privateApiEndpoint = this.clientConfigurationService.getPrivateApiEndpoint();
        return sendHttpRequest(
                requestBuilder(privateApiEndpoint, "address")
                        .header(SESSION_ID, sessionId)
                        .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build());
    }

    public HttpResponse<String> sendCredentialIssueRequest(String accessToken)
            throws IOException, InterruptedException {

        String publicApiEndpoint = this.clientConfigurationService.getPublicApiEndpoint();
        return sendHttpRequest(
                requestBuilder(publicApiEndpoint, "/credential/issue")
                        .header("x-api-key", this.clientConfigurationService.getPublicApiKey())
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build());
    }

    public HttpResponse<String> sendGetAddressesLookupRequest(String sessionId)
            throws IOException, InterruptedException {

        String privateApiEndpoint = this.clientConfigurationService.getPrivateApiEndpoint();
        return sendHttpRequest(
                requestBuilder(privateApiEndpoint, "addresses/v2")
                        .GET()
                        .header(SESSION_ID, sessionId)
                        .build());
    }

    private HttpRequest.Builder requestBuilder(String endpointType, String path) {
        return HttpRequest.newBuilder()
                .uri(
                        new URIBuilder(endpointType)
                                .setPath(this.clientConfigurationService.createUriPath(path))
                                .build())
                .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> sendGetAddressesLookupRequestWithOutSessionId()
            throws IOException, InterruptedException {

        String privateApiEndpoint = this.clientConfigurationService.getPrivateApiEndpoint();
        return sendHttpRequest(requestBuilder(privateApiEndpoint, "addresses/v2").GET().build());
    }

    public HttpResponse<String> sendNoPostCodeNoSessionIdLookUpRequest(String postcode)
            throws IOException, InterruptedException {

        String privateApiEndpoint = this.clientConfigurationService.getPrivateApiEndpoint();
        return sendHttpRequest(
                requestBuilder(privateApiEndpoint, "postcode-lookup")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        "{\"postcode\": \"" + postcode + "\"}"))
                        .build());
    }

    public HttpResponse<String> sendNoPostCodeWithSessionIdLookUpRequest(String sessionId)
            throws IOException, InterruptedException {

        String privateApiEndpoint = this.clientConfigurationService.getPrivateApiEndpoint();
        return sendHttpRequest(
                requestBuilder(privateApiEndpoint, "postcode-lookup")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .header(SESSION_ID, sessionId)
                        .build());
    }
}
