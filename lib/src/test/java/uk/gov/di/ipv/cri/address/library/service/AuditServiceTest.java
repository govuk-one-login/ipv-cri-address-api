package uk.gov.di.ipv.cri.address.library.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.address.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.address.library.exception.SqsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {
    public String SQS_QUEUE_URL = "https://example-queue-url";
    @Mock AmazonSQS mockSqs;
    @Mock ConfigurationService mockConfigurationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuditService auditService;

    @BeforeEach
    void setup() {
        when(mockConfigurationService.getSqsAuditEventQueueUrl()).thenReturn(SQS_QUEUE_URL);
        auditService = new AuditService(mockSqs, mockConfigurationService);
    }

    @Test
    void shouldSendMessageToSqsQueue() throws JsonProcessingException, SqsException {
        auditService.sendAuditEvent(AuditEventTypes.IPV_ADDRESS_CRI_START);

        ArgumentCaptor<SendMessageRequest> sqsSendMessageRequestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(mockSqs).sendMessage(sqsSendMessageRequestCaptor.capture());

        assertEquals(SQS_QUEUE_URL, sqsSendMessageRequestCaptor.getValue().getQueueUrl());

        AuditEvent messageBody =
                objectMapper.readValue(
                        sqsSendMessageRequestCaptor.getValue().getMessageBody(), AuditEvent.class);
        assertEquals(AuditEventTypes.IPV_ADDRESS_CRI_START, messageBody.getEvent());
    }
}
