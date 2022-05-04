package uk.gov.di.ipv.cri.address.library.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import uk.gov.di.ipv.cri.address.library.persistence.item.SessionItem;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataStoreTest {
    private static final String TEST_TABLE_NAME = "test-auth-code-table";

    @Mock private DynamoDbEnhancedClient mockDynamoDbEnhancedClient;
    @Mock private DynamoDbTable<SessionItem> mockDynamoDbTable;

    private SessionItem sessionItem;
    private DataStore<SessionItem> dataStore;

    @BeforeEach
    void setUp() {
        when(mockDynamoDbEnhancedClient.table(
                        anyString(), ArgumentMatchers.<TableSchema<SessionItem>>any()))
                .thenReturn(mockDynamoDbTable);

        sessionItem = new SessionItem();
        String accessToken = UUID.randomUUID().toString();

        dataStore = new DataStore<>(TEST_TABLE_NAME, SessionItem.class, mockDynamoDbEnhancedClient);
    }

    @Test
    void shouldPutItemIntoDynamoDbTable() {
        dataStore.create(sessionItem);

        ArgumentCaptor<SessionItem> authorizationCodeItemArgumentCaptor =
                ArgumentCaptor.forClass(SessionItem.class);

        verify(mockDynamoDbEnhancedClient)
                .table(eq(TEST_TABLE_NAME), ArgumentMatchers.<TableSchema<SessionItem>>any());
        verify(mockDynamoDbTable).putItem(authorizationCodeItemArgumentCaptor.capture());
        assertEquals(
                sessionItem.getSessionId(),
                authorizationCodeItemArgumentCaptor.getValue().getSessionId());
    }
}
