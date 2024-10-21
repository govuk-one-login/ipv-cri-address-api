import type { LambdaInterface } from "@aws-lambda-powertools/commons/types";
import { Logger } from "@aws-lambda-powertools/logger";
import { DynamoDbClient } from "../lib/dynamo-db-client";
import { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";
import { handleError } from "../lib/error-handler";
import { getSessionId } from "../lib/session-header";
import { AddressServiceV2 } from "./services/address-service-v2";

const logger = new Logger();
export class AddressesV2Handler implements LambdaInterface {
    public constructor(private readonly addressService: AddressServiceV2) {}

    public async handler(event: APIGatewayProxyEvent, context: Context): Promise<APIGatewayProxyResult | undefined> {
        try {
            const sessionId = getSessionId(event.headers);

            const result = await this.addressService.getAddressesBySessionId(sessionId);

            return Promise.resolve({ statusCode: 200, body: JSON.stringify({ result }) });
        } catch (error: unknown) {
            return handleError(logger, error, context.functionName);
        }
    }
}

const addressService = new AddressServiceV2(DynamoDbClient, logger);
const handlerClass = new AddressesV2Handler(addressService);
export const lambdaHandler = handlerClass.handler.bind(handlerClass);
