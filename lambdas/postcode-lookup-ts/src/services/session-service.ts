import { DynamoDBDocument } from "@aws-sdk/lib-dynamodb";
import { CriError } from "@govuk-one-login/cri-error-response";
import type { SessionItem } from "@govuk-one-login/cri-types";

import { getItem } from "./dynamodb-service.js";

interface SessionValidationDependencies {
    dynamoDbClient: DynamoDBDocument;
    sessionTableName: string;
    currentTime?: () => number;
}

export const validateSessionId = async (
    sessionId: string,
    {
        dynamoDbClient,
        sessionTableName,
        currentTime = () => Math.floor(Date.now() / 1000),
    }: SessionValidationDependencies,
): Promise<SessionItem> => {
    const result = await getItem(dynamoDbClient, sessionId, sessionTableName);

    const sessionItem = result.Item as SessionItem | undefined;

    if (!sessionItem) {
        throw new CriError(403, "Session not found");
    }

    if (sessionItem.expiryDate < currentTime()) {
        throw new CriError(403, "Session expired");
    }

    return sessionItem;
};
