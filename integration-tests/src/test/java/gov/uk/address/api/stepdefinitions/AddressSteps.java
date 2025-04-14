package gov.uk.address.api.stepdefinitions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.address.api.client.AddressApiClient;
import gov.uk.address.api.util.AddressContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.TestHarnessResponse;
import uk.gov.di.ipv.cri.common.library.stepdefinitions.CriTestContext;
import uk.gov.di.ipv.cri.common.library.util.JsonSchemaValidator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AddressSteps {
    private static final String ADDRESS_START_SCHEMA_FILE = "/schema/IPV_ADDRESS_CRI_START.json";
    private static final String ADDRESS_VC_ISSUED_SCHEMA_FILE =
            "/schema/IPV_ADDRESS_CRI_VC_ISSUED.json";
    private final ObjectMapper objectMapper;
    private final AddressApiClient addressApiClient;
    private final CriTestContext testContext;
    private final String addressStartJsonSchema;
    private final String addressVCIssuedJsonSchema;
    private AddressContext addressContext;

    public AddressSteps(
            ClientConfigurationService clientConfigurationService, CriTestContext testContext)
            throws IOException, URISyntaxException {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.addressApiClient = new AddressApiClient(clientConfigurationService);
        this.testContext = testContext;

        Path startSchemaPath =
                Paths.get(
                        Objects.requireNonNull(
                                        AddressSteps.class.getResource(ADDRESS_START_SCHEMA_FILE))
                                .toURI());
        this.addressStartJsonSchema = Files.readString(startSchemaPath);

        Path vcIssuedSchemaPath =
                Paths.get(
                        Objects.requireNonNull(
                                        AddressSteps.class.getResource(
                                                ADDRESS_VC_ISSUED_SCHEMA_FILE))
                                .toURI());
        this.addressVCIssuedJsonSchema = Files.readString(vcIssuedSchemaPath);

        this.addressContext = new AddressContext();
    }

    @When("the user performs a postcode lookup for post code {string}")
    public void theUserPerformsAPostcodeLookupForPostCode(String postcode)
            throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendPostCodeLookUpRequest(
                        this.testContext.getSessionId(),
                        URLEncoder.encode(postcode.trim(), StandardCharsets.UTF_8)));
    }

    @When("the user performs a GET postcode lookup for post code {string}")
    public void theUserPerformsAGETPostcodeLookupForPostCode(String postcode)
            throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendPostCodeLookUpGETRequest(
                        this.testContext.getSessionId(),
                        URLEncoder.encode(postcode.trim(), StandardCharsets.UTF_8)));
    }

    @Then("user receives a list of addresses containing {string}")
    public void userReceivesAListOfAddressesContaining(String postcode) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        assertEquals(200, this.testContext.getResponse().statusCode());
        assertNotNull(jsonNode.get(0).get("uprn").asText());
        assertEquals(postcode, jsonNode.get(0).get("postalCode").asText());
        addressContext.setUprn(jsonNode.get(0).get("uprn").asText());
        addressContext.setPostcode(jsonNode.get(0).get("postalCode").asText());
    }

    @When("the user selects address")
    public void theUserSelectsAddress() throws IOException, InterruptedException {
        addressContext.setCountryCode("GB");
        this.testContext.setResponse(
                this.addressApiClient.sendAddressRequest(
                        this.testContext.getSessionId(),
                        addressContext.getUprn(),
                        addressContext.getPostcode(),
                        addressContext.getCountryCode()));
    }

    @When("the user enters international address successfully")
    public void theUserEntersInternationalAddressSuccessfully(DataTable dataTable)
            throws IOException, InterruptedException {
        Map<String, String> addressData = dataTable.asMap(String.class, String.class);
        this.addressContext.setCountryCode(addressData.get("apartmentNumber"));
        this.addressContext.setRegion(addressData.get("region"));
        this.addressContext.setLocality(addressData.get("locality"));
        this.addressContext.setStreetName(addressData.get("streetName"));
        this.addressContext.setPostcode(addressData.get("postalCode"));
        this.addressContext.setBuildingName(addressData.get("buildingName"));
        this.addressContext.setBuildingNumber(addressData.get("buildingNumber"));
        this.addressContext.setYearFrom(Integer.parseInt(addressData.get("yearFrom")));
        this.addressContext.setCountryCode(addressData.get("country"));
        this.testContext.setResponse(
                this.addressApiClient.sendAddressRequest(
                        this.testContext.getSessionId(), this.addressContext));
    }

    @When("the user selects international address")
    public void theUserSelectsInternationalAddress() throws IOException, InterruptedException {
        addressContext.setCountryCode("KE");
        this.testContext.setResponse(
                this.addressApiClient.sendAddressRequest(
                        this.testContext.getSessionId(),
                        addressContext.getUprn(),
                        addressContext.getPostcode(),
                        addressContext.getCountryCode()));
    }

    @When("the user selects address without country code")
    public void theUserSelectsAddressWithoutCountryCode() throws IOException, InterruptedException {
        addressContext.setCountryCode(null);
        this.testContext.setResponse(
                this.addressApiClient.sendAddressRequest(
                        this.testContext.getSessionId(),
                        addressContext.getUprn(),
                        addressContext.getPostcode(),
                        addressContext.getCountryCode()));
    }

    @Then("the address is saved successfully")
    public void theAddressIsSavedSuccessfully() {
        assertEquals(204, this.testContext.getResponse().statusCode());
    }

    @Then("the address is not saved")
    public void theAddressIsNotSaved() {
        assertEquals(500, this.testContext.getResponse().statusCode());
    }

    @When("user sends a POST request to Credential Issue end point with a valid access token")
    public void userSendsAPostRequestToCredentialIssueEndPointWithAValidAccessToken()
            throws IOException, InterruptedException {
        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        var accessToken = jsonNode.get("access_token").asText();
        this.testContext.setResponse(this.addressApiClient.sendCredentialIssueRequest(accessToken));
    }

    @And("a valid JWT is returned in the response")
    public void aValidJWTIsReturnedInTheResponse() throws ParseException, IOException {
        assertEquals(200, this.testContext.getResponse().statusCode());
        assertNotNull(this.testContext.getResponse().body());
        makeAssertions(SignedJWT.parse(this.testContext.getResponse().body()));
    }

    @And("a valid START event is returned in the response with txma header")
    public void aValidStartEventIsReturnedInTheResponseWithTxmaHeader() throws IOException {
        String responseBody = testContext.getTestHarnessResponseBody();
        List<TestHarnessResponse<AuditEvent<Map<String, Object>>>> events =
                objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertFalse(events.isEmpty());
        assertEquals(1, events.size());

        for (TestHarnessResponse<AuditEvent<Map<String, Object>>> response : events) {
            AuditEvent<?> event = response.readAuditEvent();
            assertEquals("IPV_ADDRESS_CRI_START", event.getEvent());
            assertEquals(this.testContext.getSessionId(), event.getUser().getSessionId());
            assertEquals(
                    "deviceInformation", event.getRestricted().getDeviceInformation().getEncoded());
        }
    }

    @And("a valid START event is returned in the response without txma header")
    public void aValidStartEventIsReturnedInTheResponseWithoutTxmaHeader() throws IOException {
        String responseBody = testContext.getTestHarnessResponseBody();
        assertEquals(200, testContext.getTestHarnessResponse().httpResponse().statusCode());
        assertNotNull(responseBody);

        List<TestHarnessResponse<AuditEvent<Map<String, Object>>>> events =
                objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertFalse(events.isEmpty());
        assertEquals(1, events.size());

        for (TestHarnessResponse<AuditEvent<Map<String, Object>>> response : events) {
            AuditEvent<?> event = response.readAuditEvent();
            assertEquals("IPV_ADDRESS_CRI_START", event.getEvent());
            assertEquals(this.testContext.getSessionId(), event.getUser().getSessionId());
            assertTrue(
                    JsonSchemaValidator.validateJsonAgainstSchema(
                            response.getEvent().getData(), addressStartJsonSchema));
            assertNull(event.getRestricted());
        }
    }

    @Then("START TxMA event is validated against schema")
    public void startTxmaEventValidatedAgainstSchema() throws IOException {
        String responseBody = testContext.getTestHarnessResponseBody();

        List<TestHarnessResponse<AuditEvent<Map<String, Object>>>> testHarnessResponses =
                objectMapper.readValue(responseBody, new TypeReference<>() {});

        var events =
                testHarnessResponses.stream()
                        .filter(
                                event ->
                                        event.getEvent().toString().equals("IPV_ADDRESS_CRI_START"))
                        .collect(Collectors.toList());

        assertNotNull(events);
        for (TestHarnessResponse<AuditEvent<Map<String, Object>>> testHarnessResponse : events) {
            AuditEvent<?> event =
                    objectMapper.readValue(
                            testHarnessResponse.getEvent().getData(), AuditEvent.class);
            assertEquals(1, events.size());
            assertEquals(this.testContext.getSessionId(), event.getUser().getSessionId());
            assertTrue(
                    JsonSchemaValidator.validateJsonAgainstSchema(
                            testHarnessResponse.getEvent().getData(), addressStartJsonSchema));
        }
    }

    @Then("VC_ISSUED TxMA event is validated against schema with isUkAddress {string}")
    public void vcIssuedTxmaEventValidatedAgainstSchema(String isUkAddress) throws IOException {
        String responseBody = testContext.getTestHarnessResponseBody();

        List<TestHarnessResponse<AuditEvent<Map<String, Object>>>> testHarnessResponses =
                objectMapper.readValue(responseBody, new TypeReference<>() {});

        var events =
                testHarnessResponses.stream()
                        .filter(
                                event ->
                                        event.getEvent()
                                                .getData()
                                                .contains("IPV_ADDRESS_CRI_VC_ISSUED"))
                        .collect(Collectors.toList());

        assertNotNull(events);
        assertEquals(1, events.size());
        for (TestHarnessResponse<AuditEvent<Map<String, Object>>> testHarnessResponse : events) {
            AuditEvent<?> event =
                    objectMapper.readValue(
                            testHarnessResponse.getEvent().getData(), AuditEvent.class);
            assertEquals(this.testContext.getSessionId(), event.getUser().getSessionId());
            assertEquals(
                    Boolean.valueOf(isUkAddress),
                    ((Map<String, Object>) event.getExtensions()).get("isUkAddress"));
            assertTrue(
                    JsonSchemaValidator.validateJsonAgainstSchema(
                            testHarnessResponse.getEvent().getData(), addressVCIssuedJsonSchema));
        }
    }

    @Then("user does not get any address")
    public void user_does_not_get_any_address() throws JsonProcessingException {
        var list = objectMapper.readValue(this.testContext.getResponse().body(), List.class);
        assertTrue(list.isEmpty());
        assertEquals(200, this.testContext.getResponse().statusCode());
    }

    private void makeAssertions(SignedJWT decodedJWT) throws IOException {
        final var header = objectMapper.readTree(decodedJWT.getHeader().toString());
        final var payloadValue = decodedJWT.getPayload().toString();
        final var payload = objectMapper.readTree(payloadValue);

        assertEquals("JWT", header.at("/typ").asText());
        assertEquals("ES256", header.at("/alg").asText());
        assertEquals(
                "did:web:review-a.dev.account.gov.uk#77618cc9ebd6909cbf0b50b00126f8e7feb5dbfdb64e6c623c01a6cafca47e41",
                header.at("/kid").asText());

        assertNotNull(payload);
        assertNotNull(payload.get("nbf"));

        assertEquals("VerifiableCredential", payload.at("/vc/type/0").asText());
        assertEquals("AddressCredential", payload.at("/vc/type/1").asText());
        assertEquals(
                addressContext.getPostcode(),
                payload.at("/vc/credentialSubject/address/0/postalCode").asText());
        assertEquals(
                addressContext.getCountryCode(),
                payload.at("/vc/credentialSubject/address/0/addressCountry").asText());
        assertEquals(
                addressContext.getRegion(),
                payload.at("/vc/credentialSubject/address/0/addressRegion").asText());
    }

    @When("the user arrives at find your address page")
    public void theUserArrivesAtFindYourAddressPage() throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendGetAddressesLookupRequest(
                        this.testContext.getSessionId()));
    }

    @When("user requests lands on \\/addresses")
    public void userRequestsLandsOnAddresses() throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendGetAddressesLookupRequest(
                        this.testContext.getSessionId()));
    }

    @Then("enter your post code is pre-populated with response from \\/addresses")
    public void enterYourPostCodeIsPrePopulatedWithResponseFromAddresses()
            throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        assertEquals(200, this.testContext.getResponse().statusCode());

        assertNotNull(jsonNode);
        assertTrue(jsonNode.get("addresses").isArray());

        JsonNode firstAddress = jsonNode.get("addresses").get(0);
        assertNotNull(firstAddress);

        assertNotNull(firstAddress.get("postalCode").asText());
    }

    @Then("response should contain addresses and context from the personIdentityTable")
    public void responseShouldContainAddressesFromThePersonIdentityTableWithContext()
            throws JsonProcessingException {

        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        assertEquals(200, this.testContext.getResponse().statusCode());

        assertNotNull(jsonNode);
        JsonNode addresses = jsonNode.get("addresses");
        assertTrue(addresses.isArray());

        JsonNode firstAddress = addresses.get(0);
        assertNotNull(firstAddress);

        assertNotNull(firstAddress.get("postalCode").asText());
        assertEquals("international_user", jsonNode.get("context").asText());
    }

    @Then("responds with missing authentication")
    public void respondsMissingAuthentication() {
        assertEquals(403, this.testContext.getResponse().statusCode());
    }

    @When("the user selects previous address")
    public void the_user_selects_previous_address() throws IOException, InterruptedException {
        addressContext.setCountryCode("GB");
        this.testContext.setResponse(
                this.addressApiClient.sendPreviousAddressRequest(
                        this.testContext.getSessionId(),
                        addressContext.getUprn(),
                        addressContext.getPostcode(),
                        addressContext.getCountryCode()));
    }

    @When("a valid JWT is returned in the multiple addresses response")
    public void a_valid_jwt_is_returned_in_the_multiple_addresses_response()
            throws ParseException, IOException {
        assertEquals(200, this.testContext.getResponse().statusCode());
        assertNotNull(this.testContext.getResponse().body());
        makeAssertions(SignedJWT.parse(this.testContext.getResponse().body()));
        final SignedJWT decodedJWT = SignedJWT.parse(this.testContext.getResponse().body());
        final var payload = objectMapper.readTree(decodedJWT.getPayload().toString());

        assertEquals(
                LocalDate.now().minusMonths(3).toString(),
                payload.at("/vc/credentialSubject/address/0/validUntil").asText());
        assertEquals(
                LocalDate.now().minusMonths(3).toString(),
                payload.at("/vc/credentialSubject/address/1/validFrom").asText());
    }

    @Given(
            "a request is made to the addresses endpoint and it does not include a session_id header")
    public void aRequestIsMadeToTheAddressesEndpointWithoutSessionIdHeader()
            throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendGetAddressesLookupRequestWithOutSessionId());
    }

    @Then("the endpoint should return a 400 HTTP status code")
    public void theEndpointShouldReturnA400HttpStatusCode() {
        assertEquals(400, this.testContext.getResponse().statusCode());
    }

    @Given(
            "a request is made to the postcode-lookup endpoint with postcode and no session id in the request body")
    public void
            aRequestIsMadeToThePostcodeLookupEndpointWithPostcodeAndNoSessionIdInTheRequestBody()
                    throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendPostCodeLookupRequestWithNoSessionId("TEST"));
    }

    @Given(
            "a request is made to the postcode-lookup endpoint without a postcode and with session id in the request body")
    public void
            aRequestIsMadeToThePostcodeLookupEndpointWithoutPostcodeAndWithSessionIdInTheRequestBody()
                    throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendNoPostCodeWithSessionIdLookUpRequest(
                        this.testContext.getSessionId()));
    }

    @Then("the response body contains no session id error")
    public void theResponseBodyContainsNoSessionIdError() {
        var responseBody = this.testContext.getResponse().body();
        assertNotNull(responseBody);
        assertEquals(
                "{\"message\": \"Missing required request parameters: [session_id]\"}",
                responseBody);
    }

    @Then("the response body contains no postcode error")
    public void theResponseBodyContainsNoPostcodeError() {
        var responseBody = this.testContext.getResponse().body();
        assertNotNull(responseBody);
        assertEquals("\"Missing postcode in request body.\"", responseBody);
    }
}
