package gov.uk.address.api.stepDefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.address.api.util.PostcodeLookupResponse;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AddressApiHappyPath {

    private final String POST_CODE = "SW1A 2AA";
    private final String ENVIRONMENT = "/dev"; // dev, build, staging, integration
    private final String SESSION = ENVIRONMENT + "/session";
    private final String POSTCODE_LOOKUP = ENVIRONMENT + "/postcode-lookup/";
    private final String ADDRESS = ENVIRONMENT + "/address";
    private final String AUTHORIZATION = ENVIRONMENT + "/authorization";
    private final String TOKEN = ENVIRONMENT + "/token";
    private final String CREDENTIAL_ISSUE = ENVIRONMENT + "/credential/issue";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String userIdentityJson;
    private String ADDRESS_CRI_DEV = "address-cri-dev";
    private String sessionRequestBody;

    private HttpResponse<String> response;

    private String sessionId;
    private List<PostcodeLookupResponse> postcodeLookupResponse;
    private String authorizationCode;

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
    }

    @Then("user gets a session-id")
    public void user_gets_a_session_id() {
        assertNotNull(sessionId);
    }

    @When("the user performs a postcode lookup")
    public void theUserPerformsAPostcodeLookup() throws IOException, InterruptedException, URISyntaxException {
        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(POSTCODE_LOOKUP + POST_CODE)
                                .build())
                        .setHeader("session_id", sessionId).GET().build();

        this.response = sendHttpRequest(request);
    }

    @Then("user receives a list of addresses")
    public void userReceivesAListOfAddresses() throws IOException {
        postcodeLookupResponse = Arrays.asList(objectMapper.readValue(response.body(), PostcodeLookupResponse[].class));
        assertEquals(200, response.statusCode());
        assertFalse(postcodeLookupResponse.isEmpty());
        assertNotNull(postcodeLookupResponse.get(0).getUprn());
        assertEquals(POST_CODE, postcodeLookupResponse.get(0).getPostalCode());
    }

    @When("the user selects address")
    public void theUserSelectsAddress() throws URISyntaxException, IOException, InterruptedException {

        var requestBody = "[\n" +
                "  {\n" +
                "    \"uprn\": "+ postcodeLookupResponse.get(0).getUprn() +",\n" +
                "    \"organisationName\": \"PRIME MINISTER & FIRST LORD OF THE TREASURY\",\n" +
                "    \"departmentName\": \"\",\n" +
                "    \"subBuildingName\": \"\",\n" +
                "    \"buildingNumber\": \"10\",\n" +
                "    \"dependentStreetName\": \"\",\n" +
                "    \"doubleDependentAddressLocality\": \"\",\n" +
                "    \"dependentAddressLocality\": \"\",\n" +
                "    \"buildingName\": \"\",\n" +
                "    \"streetName\": \"DOWNING STREET\",\n" +
                "    \"addressLocality\": \"LONDON\",\n" +
                "    \"postalCode\": \"" + POST_CODE + "\",\n" +
                "    \"addressCountry\": \"GB\",\n" +
                "    \"validFrom\": \"2020-01-01\",\n" +
                "    \"validUntil\": \"\"\n" +
                "  }\n" +
                "]";

        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPrivateAPIEndpoint()).setPath(ADDRESS).build())
                        .setHeader("session_id", sessionId)
                        .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
        response = sendHttpRequest(request);
    }

    @Then("the address is saved successfully")
    public void theAddressIsSavedSuccessfully() {
        assertEquals(204, response.statusCode());
    }

    @When("user sends a GET request to authorization end point")
    public void user_sends_a_get_request_to_authorization_end_point()
            throws IOException, InterruptedException, URISyntaxException {
        var url =
                new URIBuilder(getPrivateAPIEndpoint())
                        .setPath(AUTHORIZATION)
                        .addParameter(
                                "redirect_uri",
                                "https://di-ipv-core-stub.london.cloudapps.digital/callback")
                        .addParameter("client_id", "ipv-core-stub")
                        .addParameter("response_type", "code")
                        .addParameter("scope", "openid")
                        .addParameter("state", "state-ipv")
                        .build();

        var request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .setHeader("Accept", "application/json")
                        .setHeader("session-id", sessionId)
                        .GET()
                        .build();

        response = sendHttpRequest(request);
    }

    @And("a valid authorization code is returned in the response")
    public void aValidAuthorizationCodeIsReturnedInTheResponse() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(response.body());
        assertEquals(200, response.statusCode());
        authorizationCode = jsonNode.get("authorizationCode").get("value").textValue();
        assertNotNull(authorizationCode);
    }

    @When("user sends a POST request to token end point")
    public void user_sends_a_post_request_to_token_end_point()
            throws URISyntaxException, IOException, InterruptedException {
        String privateKeyJWT =
                getPrivateKeyJWTFormParamsForAuthCode(
                        getIPVCoreStubURL(), authorizationCode.trim());
        var request =
                HttpRequest.newBuilder()
                        .uri(new URIBuilder(getPublicAPIEndpoint()).setPath(TOKEN).build())
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("x-api-key", getPublicAPIKey())
                        .POST(HttpRequest.BodyPublishers.ofString(privateKeyJWT))
                        .build();

        response = sendHttpRequest(request);
    }

    @And("a valid access token code is returned in the response")
    public void aValidAccessTokenCodeIsReturnedInTheResponse() throws IOException {
        assertEquals(200, response.statusCode());
        JsonNode jsonNode = objectMapper.readTree(response.body());
        var accessToken = jsonNode.get("access_token").asText();
        var expiresIn = jsonNode.get("expires_in").asInt();
        var tokenType = jsonNode.get("token_type").asText();
        assertEquals(3600, expiresIn);
        assertEquals("Bearer", tokenType);
        Assertions.assertFalse(accessToken.isEmpty());
    }

    @When("user sends a POST request to Credential Issue end point with a valid access token")
    public void user_sends_a_post_request_to_credential_issue_end_point_with_a_valid_access_token()
            throws IOException, InterruptedException, URISyntaxException {
        JsonNode jsonNode = objectMapper.readTree(response.body());
        var accessToken = jsonNode.get("access_token").asText();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(getPublicAPIEndpoint())
                                        .setPath(CREDENTIAL_ISSUE)
                                        .build())
                        .header("x-api-key", getPublicAPIKey())
                        .setHeader("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        response = sendHttpRequest(request);
    }

    @And("a valid JWT is returned in the response")
    public void aValidJWTIsReturnedInTheResponse() throws ParseException, IOException {
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
        makeAssertions(SignedJWT.parse(response.body()));
    }

    private void makeAssertions(SignedJWT decodedJWT) throws IOException {
        var header = decodedJWT.getHeader().toString();
        var payload = objectMapper.readTree(decodedJWT.getPayload().toString());
        JsonNode identityJSON = objectMapper.readTree(userIdentityJson);

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", header);
        assertNotNull(payload);
        assertEquals("VerifiableCredential", payload.get("vc").get("type").get(0).asText());
        assertEquals("AddressCredential", payload.get("vc").get("type").get(1).asText());

        assertEquals(POST_CODE,
                payload.get("vc")
                        .get("credentialSubject")
                        .get("address")
                        .get(0)
                        .get("postalCode")
                        .asText()
               );
    }


    private String getPublicAPIEndpoint() {
        return getApiEndpoint(
                "API_GATEWAY_ID_PUBLIC", "Environment variable PUBLIC API endpoint is not set");
    }

    private String getPublicAPIKey() {
        return Optional.ofNullable(System.getenv("APIGW_API_KEY")).orElseThrow();
    }

    private String getPrivateKeyJWTFormParamsForAuthCode(String baseUrl, String authorizationCode)
            throws URISyntaxException, IOException, InterruptedException {
        var url =
                new URIBuilder(baseUrl)
                        .setPath("backend/createTokenRequestPrivateKeyJWT")
                        .addParameter("cri", ADDRESS_CRI_DEV)
                        .addParameter("authorization_code", authorizationCode)
                        .build();

        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();
        return sendHttpRequest(request).body();
    }

}
