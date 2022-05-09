package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.di.ipv.cri.address.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.address.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.address.library.exception.SqsException;

import java.time.Instant;

public class AuditService {
    private final AmazonSQS sqs;
    private final String queueUrl;

    public AuditService(AmazonSQS sqs, ConfigurationService configurationService) {
        this.sqs = sqs;
        this.queueUrl = configurationService.getSqsAuditEventQueueUrl();
    }

    public void sendAuditEvent(AuditEventTypes eventType) throws SqsException {
        try {
            SendMessageRequest sendMessageRequest =
                    new SendMessageRequest()
                            .withQueueUrl(queueUrl)
                            .withMessageBody(generateMessageBody(eventType));
            sqs.sendMessage(sendMessageRequest);
        } catch (JsonProcessingException e) {
            throw new SqsException(e);
        }
    }

    private String generateMessageBody(AuditEventTypes eventType) throws JsonProcessingException {
        AuditEvent auditEvent = new AuditEvent();
        Instant now = Instant.now();
        int instant = (int) now.getEpochSecond();
        auditEvent.setTimestamp(instant);
        auditEvent.setEvent(eventType);
        auditEvent.setTimestampFormatted(now.toString());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(auditEvent);
    }
}
