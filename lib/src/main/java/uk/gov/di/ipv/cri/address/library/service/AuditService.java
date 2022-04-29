package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.di.ipv.cri.address.library.constants.AuditEventTypes;
import uk.gov.di.ipv.cri.address.library.exception.SqsException;
import uk.gov.di.ipv.cri.address.library.models.AuditEvent;

import java.time.Instant;
import java.util.UUID;

public class AuditService {
    private final AmazonSQS sqs;
    private final String queueUrl;

    public AuditService(AmazonSQS sqs, ConfigurationService configurationService) {
        this.sqs = sqs;
        this.queueUrl = configurationService.getSqsAuditEventQueueUrl();
    }

    public void sendAuditEvent(AuditEventTypes eventType, UUID sessionId, String clientId)
            throws SqsException {
        try {
            SendMessageRequest sendMessageRequest =
                    new SendMessageRequest()
                            .withQueueUrl(queueUrl)
                            .withMessageBody(generateMessageBody(eventType, sessionId, clientId));
            System.out.println(
                    "MESSAGE BODY ===== >>>> "
                            + sendMessageRequest.getMessageBody()
                            + " on URL===>"
                            + sendMessageRequest.getQueueUrl());
            sqs.sendMessage(sendMessageRequest);
        } catch (JsonProcessingException e) {
            throw new SqsException(e);
        }
    }

    private String generateMessageBody(AuditEventTypes eventType, UUID sessionId, String clientId)
            throws JsonProcessingException {
        AuditEvent auditEvent = new AuditEvent();
        Instant NOW = Instant.now();
        int instant = (int) NOW.getEpochSecond();
        auditEvent.setTimestamp(instant);
        auditEvent.setEvent(eventType);
        auditEvent.setClient_id(clientId);
        auditEvent.setEvent_id(sessionId.toString());
        auditEvent.setTimestamp_formatted(NOW.toString());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(auditEvent);
    }
}
