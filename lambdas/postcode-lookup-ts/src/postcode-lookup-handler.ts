import type { LambdaInterface } from "@aws-lambda-powertools/commons/types";
import { Logger } from "@aws-lambda-powertools/logger";
import { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";

import { handleError } from "./lib/error-handler";
import { PostcodeLookupService } from "./services/postcode-lookup-service";

const logger = new Logger();

export class PostcodeLookupHandler implements LambdaInterface {
    constructor(private readonly postcodeLookupService: PostcodeLookupService) {}

    public async handler(event: APIGatewayProxyEvent, context: Context): Promise<APIGatewayProxyResult | undefined> {
        try {
            return {
                statusCode: 200,
                body: JSON.stringify([]),
            };
        } catch (error: unknown) {
            return handleError(logger, error, `Error in ${context.functionName}`);
        }
    }
}

const postcodeLookupService = new PostcodeLookupService(logger);
const handlerClass = new PostcodeLookupHandler(postcodeLookupService);

export const lambdaHandler = handlerClass.handler.bind(handlerClass);
