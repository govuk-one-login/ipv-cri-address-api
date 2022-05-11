package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.di.ipv.cri.address.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.address.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.address.library.exception.SqsException;

import java.util.Date;

public class AuditService {
    private final AmazonSQS sqs;
    private final String queueUrl;
    private ConfigurationService configurationService;
    private ObjectMapper objectMapper;

    public AuditService(
            AmazonSQS sqs, ConfigurationService configurationService, ObjectMapper objectMapper) {
        this.sqs = sqs;
        this.configurationService = configurationService;
        this.queueUrl = configurationService.getSqsAuditEventQueueUrl();
        this.objectMapper = objectMapper;
    }

    public AuditService() {
        this(
                AmazonSQSClientBuilder.defaultClient(),
                new ConfigurationService(),
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule()));
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
        AuditEvent auditEvent = new AuditEvent(new Date(), eventType);
        return objectMapper.writeValueAsString(auditEvent);
    }
}
