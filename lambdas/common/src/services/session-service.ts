import { randomUUID } from "node:crypto";
import type { DynamoDBDocument } from "@aws-sdk/lib-dynamodb";
import { CriError } from "@govuk-one-login/cri-error-response";
import type { SessionItem } from "@govuk-one-login/cri-types";

import { getItem, updateAuthorizationCode } from "./dynamodb-service.js";

interface SessionValidationDependencies {
    dynamoDbClient: DynamoDBDocument;
    sessionTableName: string;
    currentTime?: () => number;
}

interface CreateAuthorizationCodeDependencies {
    dynamoDbClient: DynamoDBDocument;
    sessionTableName: string;
    getAuthorizationCodeExpirationEpoch: () => number;
    generateAuthorizationCode?: () => string;
}

export const getSessionById = async (
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

export const createAuthorizationCode = async (
    session: SessionItem,
    {
        dynamoDbClient,
        sessionTableName,
        getAuthorizationCodeExpirationEpoch,
        generateAuthorizationCode = randomUUID,
    }: CreateAuthorizationCodeDependencies,
): Promise<void> => {
    const authorizationCode = generateAuthorizationCode();

    const authorizationCodeExpiryDate = getAuthorizationCodeExpirationEpoch();

    await updateAuthorizationCode(
        dynamoDbClient,
        session.sessionId,
        authorizationCode,
        authorizationCodeExpiryDate,
        sessionTableName,
    );
};
