import type { DynamoDBDocument } from "@aws-sdk/lib-dynamodb";
import type { SessionItem, UnixMillisecondsTimestamp, UnixSecondsTimestamp } from "@govuk-one-login/cri-types";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { createAuthorizationCode, getSessionById } from "../src/services/session-service.js";

describe("session-service", () => {
    const send = vi.fn();

    const dynamoDbClient = {
        send,
    } as unknown as DynamoDBDocument;

    const sessionTableName = "session-table";
    const currentTime = 1_700_000_000;

    const validSession: SessionItem = {
        attemptCount: 0,
        createdDate: (currentTime * 1000) as UnixMillisecondsTimestamp,
        expiryDate: (currentTime + 60) as UnixSecondsTimestamp,
        sessionId: "test-session-id",
        clientId: "test-client",
        clientSessionId: "test-client-session-id",
        redirectUri: "https://example.test/callback",
        state: "test-state",
        subject: "test-subject",
        authorizationCodeExpiryDate: (currentTime + 60) as UnixSecondsTimestamp,
        accessToken: "mock-access-token",
        accessTokenExpiryDate: (currentTime + 60) as UnixSecondsTimestamp,
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe("getSessionById", () => {
        const validateSession = (sessionId: string) =>
            getSessionById(sessionId, {
                dynamoDbClient,
                sessionTableName,
                currentTime: () => currentTime,
            });

        it("uses the current time when currentTime is not provided", async () => {
            vi.spyOn(Date, "now").mockReturnValue(currentTime * 1000);

            send.mockResolvedValue({
                Item: validSession,
            });

            const result = await getSessionById("test-session-id", {
                dynamoDbClient,
                sessionTableName,
            });

            expect(result).toEqual(validSession);
        });

        it("returns a valid session", async () => {
            send.mockResolvedValue({
                Item: validSession,
            });

            const result = await validateSession("test-session-id");

            expect(result).toEqual(validSession);

            expect(send).toHaveBeenCalledOnce();
        });

        it("returns a session when it expires at the current time", async () => {
            const session = {
                ...validSession,
                expiryDate: currentTime as UnixSecondsTimestamp,
            };

            send.mockResolvedValue({
                Item: session,
            });

            const result = await validateSession("test-session-id");

            expect(result).toEqual(session);
        });

        it("throws 403 when session is not found", async () => {
            send.mockResolvedValue({});

            await expect(validateSession("missing-session-id")).rejects.toMatchObject({
                message: "Session not found",
                statusCode: 403,
            });
        });

        it("throws 403 when session has expired", async () => {
            send.mockResolvedValue({
                Item: {
                    ...validSession,
                    expiryDate: (currentTime - 1) as UnixSecondsTimestamp,
                },
            });

            await expect(validateSession("test-session-id")).rejects.toMatchObject({
                message: "Session expired",
                statusCode: 403,
            });
        });

        it("propagates DynamoDB retrieval failures", async () => {
            send.mockRejectedValue(new Error("DynamoDB unavailable"));

            await expect(validateSession("test-session-id")).rejects.toThrow(
                `Error retrieving ${sessionTableName} item with sessionId: test-session-id`,
            );
        });
    });

    describe("createAuthorizationCode", () => {
        const authorizationCode = "test-authorization-code";

        const authorizationCodeExpiryDate = 1_700_000_600;

        const generateAuthorizationCode = vi.fn(() => authorizationCode);

        const getAuthorizationCodeExpirationEpoch = vi.fn(() => authorizationCodeExpiryDate);

        it("creates and stores an authorization code", async () => {
            send.mockResolvedValue({});

            await createAuthorizationCode(validSession, {
                dynamoDbClient,
                sessionTableName,
                generateAuthorizationCode,
                getAuthorizationCodeExpirationEpoch,
            });

            expect(generateAuthorizationCode).toHaveBeenCalledOnce();

            expect(getAuthorizationCodeExpirationEpoch).toHaveBeenCalledOnce();

            expect(send).toHaveBeenCalledOnce();

            const command = send.mock.calls[0][0];

            expect(command.input).toEqual({
                TableName: sessionTableName,
                Key: {
                    sessionId: validSession.sessionId,
                },
                UpdateExpression:
                    "SET authorizationCode = :authorizationCode, authorizationCodeExpiryDate = :authorizationCodeExpiryDate",
                ConditionExpression: "attribute_exists(sessionId)",
                ExpressionAttributeValues: {
                    ":authorizationCode": authorizationCode,
                    ":authorizationCodeExpiryDate": authorizationCodeExpiryDate,
                },
            });
        });

        it("propagates DynamoDB authorization code update failures", async () => {
            send.mockRejectedValue(new Error("DynamoDB unavailable"));

            await expect(
                createAuthorizationCode(validSession, {
                    dynamoDbClient,
                    sessionTableName,
                    generateAuthorizationCode,
                    getAuthorizationCodeExpirationEpoch,
                }),
            ).rejects.toThrow(`Error updating ${sessionTableName} item with sessionId: ${validSession.sessionId}`);
        });
    });
});
