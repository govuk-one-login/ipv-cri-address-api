import { APIGatewayProxyEvent, APIGatewayProxyResult } from "aws-lambda";
import { AddressService } from "./services/address-service";
import { DynamoDbClient } from "./lib/dynamo-db-client";
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { Logger } from "@aws-lambda-powertools/logger";

const logger = new Logger();
const parameterPrefix = process.env.AWS_STACK_NAME || "";

export const lambdaHandler = async (event: APIGatewayProxyEvent): Promise<APIGatewayProxyResult> => {
    try {
        const sessionId = event.headers["session_id"];
        if (!sessionId) {
            return { statusCode: 400, body: "Missing header: session_id is required" };
        }

        const addressLookupTableName = await getParameter(`/${parameterPrefix}/AddressLookupTableName`);
        const addressService = new AddressService(addressLookupTableName, DynamoDbClient);
        const result = await addressService.getAddressesBySessionId(sessionId);

        return { statusCode: 200, body: JSON.stringify({ result }) };
    } catch (err: unknown) {
        logger.error(`An error has occurred in Addresses handler. " + ${JSON.stringify(err)}`);
        return { statusCode: 500, body: `An error has occurred. " + ${err}` };
    }
};
