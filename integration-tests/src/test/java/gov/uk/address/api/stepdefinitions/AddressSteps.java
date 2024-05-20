package gov.uk.address.api.stepdefinitions;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import gov.uk.address.api.client.AddressApiClient;
import gov.uk.address.api.util.StackProperties;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.stepdefinitions.CriTestContext;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressSteps {
    private final ObjectMapper objectMapper;
    private final AddressApiClient addressApiClient;
    private final CriTestContext testContext;
    private String uprn;
    private String postcode;

    private final AmazonSQS sqsClient =
            AmazonSQSClientBuilder.standard().withRegion(Regions.EU_WEST_2).build();

    private final String txmaQueueUrl =
            StackProperties.getOutput(
                    StackProperties.getParameter(System.getenv("STACK_NAME"), "CommonStackName"),
                    "MockAuditEventQueueUrl");

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
        ReceiveMessageRequest receiveMessageRequest =
                new ReceiveMessageRequest()
                        .withMaxNumberOfMessages(10)
                        .withQueueUrl(txmaQueueUrl)
                        .withWaitTimeSeconds(20)
                        .withVisibilityTimeout(100);

        final List<Message> startEventMessages =
                sqsClient.receiveMessage(receiveMessageRequest).getMessages().stream()
                        .filter(message -> message.getBody().contains("IPV_ADDRESS_CRI_START"))
                        .collect(Collectors.toList());

        assertFalse(startEventMessages.isEmpty());

        startEventMessages.forEach(
                message -> assertTrue(message.getBody().contains("device_information")));

        DeleteMessageBatchRequest batch =
                new DeleteMessageBatchRequest().withQueueUrl(txmaQueueUrl);
        List<DeleteMessageBatchRequestEntry> entries = batch.getEntries();

        startEventMessages.forEach(
                m ->
                        entries.add(
                                new DeleteMessageBatchRequestEntry()
                                        .withId(m.getMessageId())
                                        .withReceiptHandle(m.getReceiptHandle())));
        sqsClient.deleteMessageBatch(batch);
    }

    private void makeAssertions(SignedJWT decodedJWT) throws IOException {
        var header = decodedJWT.getHeader().toString();
        var payloadValue = decodedJWT.getPayload().toString();
        var payload = objectMapper.readTree(payloadValue);

        assertEquals("{\"typ\":\"JWT\",\"alg\":\"ES256\"}", header);

        assertNotNull(payload.get("nbf"));

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
    public void txmaEventIsAddedToTheSqsQueueNotContainingHeaderValue() throws Exception {
        final ReceiveMessageRequest receiveMessageRequest =
                new ReceiveMessageRequest()
                        .withMaxNumberOfMessages(10)
                        .withQueueUrl(txmaQueueUrl)
                        .withWaitTimeSeconds(20)
                        .withVisibilityTimeout(100);

        ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
        List<Message> sqsMessageList = null;
        if (receiveMessageResult != null
                && receiveMessageResult.getMessages() != null
                && receiveMessageResult.getMessages().size() > 0) {
            sqsMessageList = receiveMessageResult.getMessages();

            for (Message sqsMessage : sqsMessageList) {
                String receivedMessageBody = sqsMessage.getBody();
                if (receivedMessageBody.contains("IPV_ADDRESS_CRI_START")) {
                    assertFalse(receivedMessageBody.contains("device_information"));
                } else System.out.println("START event not found");
            }
        } else throw new Exception("RecieveMessageResult is empty");

        DeleteMessageBatchRequest batch =
                new DeleteMessageBatchRequest().withQueueUrl(txmaQueueUrl);
        List<DeleteMessageBatchRequestEntry> entries = batch.getEntries();

        sqsMessageList.forEach(
                m ->
                        entries.add(
                                new DeleteMessageBatchRequestEntry()
                                        .withId(m.getMessageId())
                                        .withReceiptHandle(m.getReceiptHandle())));
        sqsClient.deleteMessageBatch(batch);
    }

    @Given("the SQS events are purged from the queue")
    public void the_sqs_events_are_purged_from_the_queue() {
        PurgeQueueRequest pqRequest = new PurgeQueueRequest(txmaQueueUrl);
        sqsClient.purgeQueue(pqRequest);
    }
}
