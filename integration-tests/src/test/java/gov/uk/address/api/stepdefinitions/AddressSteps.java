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
                        this.testContext.getSessionId(), postcode));
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
        this.testContext.setResponse(
                this.addressApiClient.sendAddressRequest(
                        this.testContext.getSessionId(), uprn, postcode));
    }

    @Then("the address is saved successfully")
    public void theAddressIsSavedSuccessfully() {
        assertEquals(204, this.testContext.getResponse().statusCode());
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
        final var header = decodedJWT.getHeader().toString();
        final var payloadValue = decodedJWT.getPayload().toString();
        final var payload = objectMapper.readTree(payloadValue);

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", header);

        assertNotNull(payload);
        assertNotNull(payload.get("nbf"));

        assertEquals("VerifiableCredential", payload.at("/vc/type/0").asText());
        assertEquals("AddressCredential", payload.at("/vc/type/1").asText());
        assertEquals(postcode, payload.at("/vc/credentialSubject/address/0/postalCode").asText());
    }
}
