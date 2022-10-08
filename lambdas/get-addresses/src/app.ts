import { APIGatewayProxyEvent, APIGatewayProxyResult } from "aws-lambda";
import { AddressService } from "./services/address-service";
import config from "./config";
import { DynamoDbClient } from "./lib/dynamo-db-client";

export const lambdaHandler = async (event: APIGatewayProxyEvent): Promise<APIGatewayProxyResult> => {
    let response: APIGatewayProxyResult;
    try {
        const sessionId = event.headers["session_id"] as string;
        if (!sessionId) {
            response = {
                statusCode: 400,
                body: "Missing header: session_id is required",
            };
            return response;
        }

        const addressService = new AddressService(config.addressLookupStorageTableName, DynamoDbClient);
        const result = await addressService.getAddressesBySessionId(sessionId);

        response = {
            statusCode: 200,
            body: JSON.stringify({
                result,
            }),
        };
    } catch (err) {
        // eslint-disable-next-line no-console
        console.error(err);
        response = {
            statusCode: 500,
            body: "An error has occurred. " + err,
        };
    }
    return response;
};
