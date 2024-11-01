package gov.uk.address.api.stepdefinitions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.address.api.client.AddressApiClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.gov.di.ipv.cri.common.library.aws.CloudFormationHelper;
import uk.gov.di.ipv.cri.common.library.aws.SQSHelper;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.stepdefinitions.CriTestContext;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressSteps {
    private final ObjectMapper objectMapper;
    private final AddressApiClient addressApiClient;
    private final CriTestContext testContext;
    private final SQSHelper sqs;

    private String postcode;
    private String uprn;
    private String countryCode;

    private final String auditEventQueueUrl =
            CloudFormationHelper.getOutput(
                    CloudFormationHelper.getParameter(System.getenv("STACK_NAME"), "TxmaStackName"),
                    "AuditEventQueueUrl");

    public AddressSteps(
            ClientConfigurationService clientConfigurationService, CriTestContext testContext) {
        this.objectMapper = new ObjectMapper();
        this.addressApiClient = new AddressApiClient(clientConfigurationService);
        this.testContext = testContext;
        this.sqs = new SQSHelper(null, this.objectMapper);
    }

    @When("the user performs a postcode lookup for post code {string}")
    public void theUserPerformsAPostcodeLookupForPostCode(String postcode)
            throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendPostCodeLookUpRequest(
                        this.testContext.getSessionId(),
                        URLEncoder.encode(postcode.trim(), StandardCharsets.UTF_8)));
    }

    @Then("user receives a list of addresses containing {string}")
    public void userReceivesAListOfAddressesContaining(String postcode) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        assertEquals(200, this.testContext.getResponse().statusCode());
        assertNotNull(jsonNode.get(0).get("uprn").asText());
        uprn = jsonNode.get(0).get("uprn").asText();
        assertEquals(postcode, jsonNode.get(0).get("postalCode").asText());
        this.postcode = jsonNode.get(0).get("postalCode").asText();
    }

    @When("the user selects address")
    public void theUserSelectsAddress() throws IOException, InterruptedException {
        countryCode = "GB";
        this.testContext.setResponse(
                this.addressApiClient.sendAddressRequest(
                        this.testContext.getSessionId(), uprn, postcode, countryCode));
    }

    @When("the user selects international address")
    public void theUserSelectsInternationalAddress() throws IOException, InterruptedException {
        countryCode = "IA";
        this.testContext.setResponse(
                this.addressApiClient.sendAddressRequest(
                        this.testContext.getSessionId(), uprn, postcode, countryCode));
    }

    @When("the user selects address without country code")
    public void theUserSelectsAddressWithoutCountryCode() throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendAddressRequest(
                        this.testContext.getSessionId(), uprn, postcode, null));
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

    @Then("TXMA event is added to the SQS queue containing device information header")
    public void txmaEventIsAddedToSqsQueueContainingDeviceInformationHeader()
            throws IOException, InterruptedException {
        assertEquals("deviceInformation", getDeviceInformationHeader());
    }

    @Then("TXMA event is added to the SQS queue not containing device information header")
    public void txmaEventIsAddedToSqsQueueNotContainingDeviceInformationHeader()
            throws InterruptedException, IOException {
        assertEquals("", getDeviceInformationHeader());
    }

    @And("{int} events are deleted from the audit events SQS queue")
    public void deleteEventsFromSqsQueue(int messageCount) throws InterruptedException {
        this.sqs.deleteMatchingMessages(
                auditEventQueueUrl,
                messageCount,
                Collections.singletonMap("/user/session_id", testContext.getSessionId()));
    }

    private String getDeviceInformationHeader() throws InterruptedException, IOException {
        final List<Message> startEventMessages =
                this.sqs.receiveMatchingMessages(
                        auditEventQueueUrl,
                        1,
                        Map.ofEntries(
                                entry("/event_name", "IPV_ADDRESS_CRI_START"),
                                entry("/user/session_id", testContext.getSessionId())));

        assertEquals(1, startEventMessages.size());

        return objectMapper
                .readTree(startEventMessages.get(0).body())
                .at("/restricted/device_information/encoded")
                .asText();
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
        assertEquals(postcode, payload.at("/vc/credentialSubject/address/0/postalCode").asText());
        assertEquals(
                countryCode, payload.at("/vc/credentialSubject/address/0/addressCountry").asText());
        assertEquals(
                "DummyRegion",
                payload.at("/vc/credentialSubject/address/0/addressRegion").asText());
    }

    @When("the user arrives at find your address page")
    public void theUserArrivesAtFindYourAddressPage() throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendGetAddressesLookupRequest(
                        this.testContext.getSessionId()));
    }

    @When("user requests lands on \\/addresses\\/v2")
    public void userRequestsLandsOnAddressesV2() throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.addressApiClient.sendGetAddressesLookupRequestV2(
                        this.testContext.getSessionId()));
    }

    @Then("enter your post code is pre-populated with response from \\/addresses")
    public void enterYourPostCodeIsPrePopulatedWithResponseFromAddresses()
            throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        assertEquals(200, this.testContext.getResponse().statusCode());

        assertNotNull(jsonNode);
        assertTrue(jsonNode.isArray());

        JsonNode firstAddress = jsonNode.get(0);
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
}
