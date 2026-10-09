import { DynamoDBDocument } from "@aws-sdk/lib-dynamodb";
import { Logger } from "@aws-lambda-powertools/logger";

import { ApiError } from "../lib/error-handler";
import { SessionItem } from "../types/session";
import { DynamoDbService } from "./dynamodb-service";

export class SessionService {
    private readonly dynamoDbService: DynamoDbService;

    constructor(
        dynamoDbClient: DynamoDBDocument,
        logger: Logger,
        private readonly sessionTableName: string,
        private readonly currentTime: () => number = () => Math.floor(Date.now() / 1000),
    ) {
        this.dynamoDbService = new DynamoDbService(dynamoDbClient, logger);
    }

    public async validateSessionId(sessionId: string): Promise<SessionItem> {
        const result = await this.dynamoDbService.getItem(sessionId, this.sessionTableName);

        const sessionItem = result.Item as SessionItem | undefined;

        if (!sessionItem) {
            throw new ApiError("Session not found", 403);
        }

        if (sessionItem.expiryDate <= this.currentTime()) {
            throw new ApiError("Session expired", 403);
        }

        if (!sessionItem.clientId) {
            throw new ApiError("The Client ID provided for this session is not supported", 400);
        }

        return sessionItem;
    }
}
