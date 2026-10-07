import type { LambdaInterface } from "@aws-lambda-powertools/commons/types";
import { Logger } from "@aws-lambda-powertools/logger";
import { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";

import { ApiError, handleError } from "./lib/error-handler";
import { PostcodeLookupService } from "./services/postcode-lookup-service";
import { PostcodeRequest } from "./types/postcode-request";
import { getSessionId } from "./lib/session-header";
import type { SessionService } from "./services/session-service";

const logger = new Logger();

export class PostcodeLookupHandler implements LambdaInterface {
    constructor(
        private readonly postcodeLookupService: PostcodeLookupService,
        private readonly sessionService: SessionService,
    ) {}
    private getPostcodeFromRequest(event: APIGatewayProxyEvent): string {
        if (!event.body) {
            throw new ApiError("Missing postcode in request body", 400);
        }

        let request: PostcodeRequest;

        try {
            request = JSON.parse(event.body) as PostcodeRequest;
        } catch {
            throw new ApiError("Failed to parse postcode from request body", 400);
        }

        if (!request.postcode?.trim()) {
            throw new ApiError("Missing postcode in request body", 400);
        }

        return request.postcode;
    }

    public async handler(event: APIGatewayProxyEvent, context: Context): Promise<APIGatewayProxyResult | undefined> {
        try {
            const postcode = this.getPostcodeFromRequest(event);

            const sessionId = getSessionId(event.headers);

            const session = await this.sessionService.validateSessionId(sessionId);

            const results = await this.postcodeLookupService.lookupPostcode(postcode, session.clientId);

            return {
                statusCode: 200,
                body: JSON.stringify(results),
            };
        } catch (error: unknown) {
            return handleError(logger, error, `Error in ${context.functionName}`);
        }
    }
}
