package uk.gov.di.ipv.cri.address.library.persistance;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.AddressSessionItem;

@ExtendWith(MockitoExtension.class)
class DataStoreTest {
    private static final String TEST_TABLE_NAME = "test-auth-code-table";

    @Mock private DynamoDbEnhancedClient mockDynamoDbEnhancedClient;
    @Mock private DynamoDbTable<AddressSessionItem> mockDynamoDbTable;

    private AddressSessionItem accessTokenItem;
    private DataStore<AddressSessionItem> dataStore;

    //    @BeforeEach
    //    void setUp() {
    //        when(mockDynamoDbEnhancedClient.table(
    //                        anyString(), ArgumentMatchers.<TableSchema<AddressSessionItem>>any()))
    //                .thenReturn(mockDynamoDbTable);
    //
    //        accessTokenItem = new AddressSessionItem();
    //        String accessToken = UUID.randomUUID().toString();
    //        accessTokenItem.setAccessToken(accessToken);
    //        accessTokenItem.setResourceId("test-resource-12345");
    //
    //        dataStore =
    //                new DataStore<>(
    //                        TEST_TABLE_NAME, AddressSessionItem.class,
    // mockDynamoDbEnhancedClient);
    //    }

    //    @Test
    //    void shouldPutItemIntoDynamoDbTable() {
    //        dataStore.create(accessTokenItem);
    //
    //        ArgumentCaptor<AddressSessionItem> authorizationCodeItemArgumentCaptor =
    //                ArgumentCaptor.forClass(AddressSessionItem.class);
    //
    //        verify(mockDynamoDbEnhancedClient)
    //                .table(
    //                        eq(TEST_TABLE_NAME),
    //                        ArgumentMatchers.<TableSchema<AddressSessionItem>>any());
    //        verify(mockDynamoDbTable).putItem(authorizationCodeItemArgumentCaptor.capture());
    //        assertEquals(
    //                accessTokenItem.getAccessToken(),
    //                authorizationCodeItemArgumentCaptor.getValue().getAccessToken());
    //        assertEquals(
    //                accessTokenItem.getResourceId(),
    //                authorizationCodeItemArgumentCaptor.getValue().getResourceId());
    //    }

    //    @Test
    //    void shouldGetItemFromDynamoDbTableViaPartitionKeyAndSortKey() {
    //        dataStore.getItem("partition-key-12345", "sort-key-12345");
    //
    //        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);
    //
    //        verify(mockDynamoDbEnhancedClient)
    //                .table(
    //                        eq(TEST_TABLE_NAME),
    //                        ArgumentMatchers.<TableSchema<AddressSessionItem>>any());
    //        verify(mockDynamoDbTable).getItem(keyCaptor.capture());
    //        assertEquals("partition-key-12345", keyCaptor.getValue().partitionKeyValue().s());
    //        assertEquals("sort-key-12345", keyCaptor.getValue().sortKeyValue().get().s());
    //    }

    //    @Test
    //    void shouldGetItemFromDynamoDbTableViaPartitionKey() {
    //        dataStore.getItem("partition-key-12345");
    //
    //        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);
    //
    //        verify(mockDynamoDbEnhancedClient)
    //                .table(
    //                        eq(TEST_TABLE_NAME),
    //                        ArgumentMatchers.<TableSchema<AddressSessionItem>>any());
    //        verify(mockDynamoDbTable).getItem(keyCaptor.capture());
    //        assertEquals("partition-key-12345", keyCaptor.getValue().partitionKeyValue().s());
    //        assertTrue(keyCaptor.getValue().sortKeyValue().isEmpty());
    //    }

    //    @Test
    //    void shouldDeleteItemFromDynamoDbTableViaPartitionKeyAndSortKey() {
    //        dataStore.delete("partition-key-12345", "sort-key-12345");
    //
    //        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);
    //
    //        verify(mockDynamoDbEnhancedClient)
    //                .table(
    //                        eq(TEST_TABLE_NAME),
    //                        ArgumentMatchers.<TableSchema<AddressSessionItem>>any());
    //        verify(mockDynamoDbTable).deleteItem(keyCaptor.capture());
    //        assertEquals("partition-key-12345", keyCaptor.getValue().partitionKeyValue().s());
    //        assertEquals("sort-key-12345", keyCaptor.getValue().sortKeyValue().get().s());
    //    }

    //    @Test
    //    void shouldDeleteItemFromDynamoDbTableViaPartitionKey() {
    //        dataStore.delete("partition-key-12345");
    //
    //        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);
    //
    //        verify(mockDynamoDbEnhancedClient)
    //                .table(
    //                        eq(TEST_TABLE_NAME),
    //                        ArgumentMatchers.<TableSchema<AddressSessionItem>>any());
    //        verify(mockDynamoDbTable).deleteItem(keyCaptor.capture());
    //        assertEquals("partition-key-12345", keyCaptor.getValue().partitionKeyValue().s());
    //        assertTrue(keyCaptor.getValue().sortKeyValue().isEmpty());
    //    }
}
