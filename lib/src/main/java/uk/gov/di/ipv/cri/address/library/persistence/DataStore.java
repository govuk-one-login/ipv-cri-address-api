package uk.gov.di.ipv.cri.address.library.persistence;

import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DataStore<T> {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final String tableName;
    private final Class<T> typeParameterClass;

    public DataStore(
            String tableName,
            Class<T> typeParameterClass,
            DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.tableName = tableName;
        this.typeParameterClass = typeParameterClass;
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    public static DynamoDbEnhancedClient getClient(URI endpointOverride) {
        DynamoDbClientBuilder clientBuilder =
                getDynamoDbClientBuilder(
                        DynamoDbClient.builder().endpointOverride(endpointOverride));

        return DynamoDbEnhancedClient.builder().dynamoDbClient(clientBuilder.build()).build();
    }

    public static DynamoDbEnhancedClient getClient() {
        DynamoDbClientBuilder clientBuilder = getDynamoDbClientBuilder(DynamoDbClient.builder());

        return DynamoDbEnhancedClient.builder().dynamoDbClient(clientBuilder.build()).build();
    }

    public void create(T item) {
        getTable().putItem(item);
    }

    public DynamoDbTable<T> getTable() {
        return dynamoDbEnhancedClient.table(
                tableName, TableSchema.fromBean(this.typeParameterClass));
    }

    public T getItem(String partitionValue, String sortValue) {
        return getItemByKey(
                Key.builder().partitionValue(partitionValue).sortValue(sortValue).build());
    }

    public T getItem(String partitionValue) {
        return getItemByKey(Key.builder().partitionValue(partitionValue).build());
    }

    public List<T> getItems(String partitionValue) {
        return getTable()
                .query(
                        QueryConditional.keyEqualTo(
                                Key.builder().partitionValue(partitionValue).build()))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public <T> List<T> getItemByGsi(DynamoDbIndex<T> index, String value) {
        AttributeValue attVal = AttributeValue.builder().s(value).build();
        QueryConditional queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());

        SdkIterable<Page<T>> items =
                index.query(
                        QueryEnhancedRequest.builder().queryConditional(queryConditional).build());

        List<T> item =
                items.stream().map(Page::items).findFirst().orElseGet(Collections::emptyList);

        return item;
    }

    public T update(T item) {
        return getTable().updateItem(item);
    }

    public T delete(String partitionValue, String sortValue) {
        return delete(Key.builder().partitionValue(partitionValue).sortValue(sortValue).build());
    }

    public T delete(String partitionValue) {
        return delete(Key.builder().partitionValue(partitionValue).build());
    }

    private T getItemByKey(Key key) {
        return getTable().getItem(key);
    }

    private T delete(Key key) {
        return getTable().deleteItem(key);
    }

    private static DynamoDbClientBuilder getDynamoDbClientBuilder(DynamoDbClientBuilder builder) {
        return builder.httpClient(UrlConnectionHttpClient.create()).region(Region.EU_WEST_2);
    }
}
