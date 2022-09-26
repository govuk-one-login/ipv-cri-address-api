package gov.uk.address.api.stepDefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressApiHappyPath {
    public final String ENVIRONMENT = "/dev"; // dev, build, staging, integration
    public final String SESSION = ENVIRONMENT + "/session";
    private final String POSTCODE_LOOKUP = ENVIRONMENT + "/postcode-lookup/";

    private String userIdentityJson;
    public final String ADDRESS_CRI_DEV = "address-cri-dev";
    private String sessionRequestBody;

    private HttpResponse<String> response;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String sessionId;

    private String getPrivateAPIEndpoint() {
        return getApiEndpoint(
                "API_GATEWAY_ID_PRIVATE", "Environment variable PRIVATE API endpoint is not set");
    }

    private String getApiEndpoint(String apikey, String message) {
        String apiEndpoint = System.getenv(apikey);
        Optional.ofNullable(apiEndpoint).orElseThrow(() -> new IllegalArgumentException(message));

        return "https://" + apiEndpoint + ".execute-api.eu-west-2.amazonaws.com";
    }

    @Given("user has the user identity in the form of a signed JWT string")
    public void userHasTheUserIdentityInTheFormOfASignedJWTString()
            throws URISyntaxException, IOException, InterruptedException {
        int experianRowNumber = 681;
        userIdentityJson = getClaimsForUser(getIPVCoreStubURL(), experianRowNumber);
        sessionRequestBody = createRequest(getIPVCoreStubURL(), userIdentityJson);
        System.out.println("sessionRequestBody = " + sessionRequestBody);
    }

    private String getClaimsForUser(String baseUrl, int userDataRowNumber)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(baseUrl)
                                        .setPath("backend/generateInitialClaimsSet")
                                        .addParameter("cri", ADDRESS_CRI_DEV)
                                        .addParameter(
                                                "rowNumber", String.valueOf(userDataRowNumber))
                                        .build())
                        .GET()
                        .build();
        return sendHttpRequest(request).body();
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {

        String basicAuthUser =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_USER"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_USER is not set");
        String basicAuthPassword =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_PASSWORD"),
                        "Environment variable IPV_CORE_STUB_BASIC_AUTH_PASSWORD is not set");

        HttpClient client =
                HttpClient.newBuilder()
                        .authenticator(
                                new Authenticator() {
                                    @Override
                                    protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(
                                                basicAuthUser, basicAuthPassword.toCharArray());
                                    }
                                })
                        .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getIPVCoreStubURL() {
        return Optional.ofNullable(System.getenv("IPV_CORE_STUB_URL"))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Environment variable IPV_CORE_STUB_URL is not set"));
    }

    private String createRequest(String baseUrl, String jsonString)
            throws URISyntaxException, IOException, InterruptedException {

        var uri =
                new URIBuilder(baseUrl)
                        .setPath("backend/createSessionRequest")
                        .addParameter("cri", ADDRESS_CRI_DEV)
                        .build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                        .build();

        return sendHttpRequest(request).body();
    }

    @When("user sends a POST request to session end point")
    public void user_sends_a_post_request_to_session_end_point()
            throws URISyntaxException, IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(SESSION).build())
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(sessionRequestBody))
                        .build();
        this.response = sendHttpRequest(request);

        Map<String, String> deserializedResponse =
                objectMapper.readValue(this.response.body(), new TypeReference<>() {});
        sessionId = deserializedResponse.get("session_id");
        System.out.println("response = " + response);
    }

    @Then("user gets a session-id")
    public void user_gets_a_session_id() {
        assertNotNull(sessionId);
        System.out.println("SESSION_ID = " + sessionId);
    }

    @When("the user performs a postcode lookup")
    public void theUserPerformsAPostcodeLookup() throws IOException, InterruptedException, URISyntaxException {
        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(POSTCODE_LOOKUP + "SW1A 2AA")
                                .build())
                        .setHeader("session_id", sessionId).GET().build();

        this.response = sendHttpRequest(request);
        System.out.println("this.response = " + this.response);
    }

    @Then("user receives a list of addresses")
    public void userReceivesAListOfAddresses() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(response.body());
        assertTrue(jsonNode.iterator().hasNext());
    }
}
