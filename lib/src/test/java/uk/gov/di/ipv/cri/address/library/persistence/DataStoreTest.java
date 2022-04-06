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
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

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
    @Mock private DynamoDbTable<AddressSessionItem> mockDynamoDbTable;

    private AddressSessionItem addressSessionItem;
    private DataStore<AddressSessionItem> dataStore;

    @BeforeEach
    void setUp() {
        when(mockDynamoDbEnhancedClient.table(
                        anyString(), ArgumentMatchers.<TableSchema<AddressSessionItem>>any()))
                .thenReturn(mockDynamoDbTable);

        addressSessionItem = new AddressSessionItem();
        String accessToken = UUID.randomUUID().toString();

        dataStore =
                new DataStore<>(
                        TEST_TABLE_NAME, AddressSessionItem.class, mockDynamoDbEnhancedClient);
    }

    @Test
    void shouldPutItemIntoDynamoDbTable() {
        dataStore.create(addressSessionItem);

        ArgumentCaptor<AddressSessionItem> authorizationCodeItemArgumentCaptor =
                ArgumentCaptor.forClass(AddressSessionItem.class);

        verify(mockDynamoDbEnhancedClient)
                .table(
                        eq(TEST_TABLE_NAME),
                        ArgumentMatchers.<TableSchema<AddressSessionItem>>any());
        verify(mockDynamoDbTable).putItem(authorizationCodeItemArgumentCaptor.capture());
        assertEquals(
                addressSessionItem.getSessionId(),
                authorizationCodeItemArgumentCaptor.getValue().getSessionId());
    }
}
