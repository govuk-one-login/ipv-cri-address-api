import type { LambdaInterface } from "@aws-lambda-powertools/commons/types";
import { Logger } from "@aws-lambda-powertools/logger";
import { DynamoDbClient } from "./lib/dynamo-db-client";
import { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";
import { handleError } from "./lib/error-handler";
import { getSessionId } from "./lib/session-header";
import { AddressService } from "./services/address-service";

const logger = new Logger();
export class AddressesHandler implements LambdaInterface {
    public constructor(private readonly addressService: AddressService) {}

    public async handler(event: APIGatewayProxyEvent, context: Context): Promise<APIGatewayProxyResult | undefined> {
        try {
            const sessionId = getSessionId(event.headers);

            const result = await this.addressService.getAddressesBySessionId(sessionId);

            return Promise.resolve({ statusCode: 200, body: JSON.stringify(result) });
        } catch (error: unknown) {
            return handleError(logger, error as Error, `Error in ${context.functionName}`);
        }
    }
}

const addressService = new AddressService(DynamoDbClient, logger);
const handlerClass = new AddressesHandler(addressService);
export const lambdaHandler = handlerClass.handler.bind(handlerClass);
