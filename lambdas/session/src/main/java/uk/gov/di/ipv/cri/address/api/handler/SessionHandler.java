package uk.gov.di.ipv.cri.address.api.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

public class SessionHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent =
                new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setStatusCode(HttpStatus.SC_CREATED);
        UUID uuid = UUID.randomUUID();
        try {
            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
            DynamoDB dynamoDB = new DynamoDB(client);
            String tableName = System.getenv("TableName");
            Table table = dynamoDB.getTable(tableName);
            Item item = new Item().withPrimaryKey("session-id", uuid.toString());
            table.putItem(item);
        } catch (Exception e) {
            apiGatewayProxyResponseEvent.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            apiGatewayProxyResponseEvent.setBody(e.getMessage());
            return apiGatewayProxyResponseEvent;
        }

        Map<String, String> responseMap = Map.of("session_id", uuid.toString());
        apiGatewayProxyResponseEvent.setBody(new Gson().toJson(responseMap));
        return apiGatewayProxyResponseEvent;
    }
}
