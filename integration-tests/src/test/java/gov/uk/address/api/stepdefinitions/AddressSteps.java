package gov.uk.address.api.stepdefinitions;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.address.api.client.AddressApiClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.stepdefinitions.CriTestContext;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AddressSteps {

    private final ObjectMapper objectMapper;
    private final AddressApiClient addressApiClient;
    private final CriTestContext testContext;
    private String uprn;
    private String postcode;

    private static final String TXMA_QUEUE_URL = System.getenv("TXMA_QUEUE_URL");

    public AddressSteps(
            ClientConfigurationService clientConfigurationService, CriTestContext testContext) {
        this.objectMapper = new ObjectMapper();
        this.addressApiClient = new AddressApiClient(clientConfigurationService);
        this.testContext = testContext;
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

    @Then("TXMA event is added to the sqs queue containing header value")
    public void txma_event_is_added_to_the_sqs_queue() {
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.EU_WEST_2).build();

        ReceiveMessageRequest receiveMessageRequest =
                new ReceiveMessageRequest()
                        .withMaxNumberOfMessages(10)
                        .withQueueUrl(TXMA_QUEUE_URL)
                        .withWaitTimeSeconds(20)
                        .withVisibilityTimeout(20);
        ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(receiveMessageRequest);
        if (receiveMessageResult != null
                && receiveMessageResult.getMessages() != null
                && receiveMessageResult.getMessages().size() > 0) {
            List<Message> sqsMessageList = receiveMessageResult.getMessages();

            for (Message sqsMessage : sqsMessageList) {
                String receivedMessageBody = sqsMessage.getBody();
                if (receivedMessageBody.contains("IPV_ADDRESS_CRI_START")) {
                    assertTrue(receivedMessageBody.contains("device_information"));
                    System.out.println("receivedMessageBody = " + receivedMessageBody);
                } else System.out.println("START event not found");
            }
        }
    }

    private void makeAssertions(SignedJWT decodedJWT) throws IOException {
        var header = decodedJWT.getHeader().toString();
        var payloadValue = decodedJWT.getPayload().toString();
        var payload = objectMapper.readTree(payloadValue);

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", header);

        assertNotNull(payload.get("nbf"));
        //        assertNotNull(payload.get("exp"));
        //        long expectedJwtTtl = 2L * 60L * 60L;
        //        assertEquals(expectedJwtTtl, payload.get("exp").asLong() -
        // payload.get("nbf").asLong());

        assertNotNull(payload);
        assertEquals("VerifiableCredential", payload.get("vc").get("type").get(0).asText());
        assertEquals("AddressCredential", payload.get("vc").get("type").get(1).asText());

        assertEquals(
                postcode,
                payload.get("vc")
                        .get("credentialSubject")
                        .get("address")
                        .get(0)
                        .get("postalCode")
                        .asText());
    }

    @Then("user does not get any address")
    public void user_does_not_get_any_address() throws JsonProcessingException {
        var list = objectMapper.readValue(this.testContext.getResponse().body(), List.class);
        assertTrue(list.isEmpty());
        assertEquals(200, this.testContext.getResponse().statusCode());
    }

    @Then("TXMA event is added to the sqs queue not containing header value")
    public void txmaEventIsAddedToTheSqsQueueNotContainingHeaderValue() {
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.EU_WEST_2).build();

        ReceiveMessageRequest receiveMessageRequest =
                new ReceiveMessageRequest()
                        .withMaxNumberOfMessages(10)
                        .withQueueUrl(TXMA_QUEUE_URL)
                        .withWaitTimeSeconds(20)
                        .withVisibilityTimeout(20);
        ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(receiveMessageRequest);
        if (receiveMessageResult != null
                && receiveMessageResult.getMessages() != null
                && receiveMessageResult.getMessages().size() > 0) {
            List<Message> sqsMessageList = receiveMessageResult.getMessages();

            for (Message sqsMessage : sqsMessageList) {
                String receivedMessageBody = sqsMessage.getBody();
                if (receivedMessageBody.contains("IPV_ADDRESS_CRI_START")) {
                    assertFalse(receivedMessageBody.contains("device_information"));
                } else System.out.println("START event not found");
            }
        }
    }

    @And("SQS events have been deleted")
    public void sqsEventsHaveBeenDeleted() {
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.EU_WEST_2).build();

        PurgeQueueRequest pqRequest = new PurgeQueueRequest(TXMA_QUEUE_URL);
        sqs.purgeQueue(pqRequest);
    }
}
