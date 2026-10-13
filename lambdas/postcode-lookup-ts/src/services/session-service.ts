import { DynamoDBDocument } from "@aws-sdk/lib-dynamodb";
import { CriError } from "@govuk-one-login/cri-error-response";
import type { SessionItem } from "@govuk-one-login/cri-types";

import { DynamoDbService } from "./dynamodb-service";

export class SessionService {
    private readonly dynamoDbService: DynamoDbService;

    constructor(
        dynamoDbClient: DynamoDBDocument,
        private readonly sessionTableName: string,
        private readonly currentTime: () => number = () => Math.floor(Date.now() / 1000),
    ) {
        this.dynamoDbService = new DynamoDbService(dynamoDbClient);
    }

    public async validateSessionId(sessionId: string): Promise<SessionItem> {
        const result = await this.dynamoDbService.getItem(sessionId, this.sessionTableName);

        const sessionItem = result.Item as SessionItem | undefined;

        if (!sessionItem) {
            throw new CriError(403, "Session not found");
        }

        if (sessionItem.expiryDate <= this.currentTime()) {
            throw new CriError(403, "Session expired");
        }

        if (!sessionItem.clientId) {
            throw new CriError(400, "The Client ID provided for this session is not supported");
        }

        return sessionItem;
    }
}
