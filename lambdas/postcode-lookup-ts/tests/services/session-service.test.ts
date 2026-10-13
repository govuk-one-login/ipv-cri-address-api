import { DynamoDBDocument } from "@aws-sdk/lib-dynamodb";
import type { SessionItem, UnixMillisecondsTimestamp, UnixSecondsTimestamp } from "@govuk-one-login/cri-types";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { SessionService } from "../../src/services/session-service";

describe("SessionService", () => {
    const send = vi.fn();

    const mockDynamoDbClient = {
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

    it("returns a valid session", async () => {
        send.mockResolvedValue({
            Item: validSession,
        });

        const service = new SessionService(mockDynamoDbClient, sessionTableName, () => currentTime);

        const result = await service.validateSessionId("test-session-id");

        expect(result).toEqual(validSession);
        expect(send).toHaveBeenCalledOnce();
    });

    it("throws 403 when session is not found", async () => {
        send.mockResolvedValue({});

        const service = new SessionService(mockDynamoDbClient, sessionTableName, () => currentTime);

        await expect(service.validateSessionId("missing-session-id")).rejects.toMatchObject({
            message: "Session not found",
            statusCode: 403,
        });
    });

    it("throws 403 when session has expired", async () => {
        send.mockResolvedValue({
            Item: {
                ...validSession,
                expiryDate: currentTime,
            },
        });

        const service = new SessionService(mockDynamoDbClient, sessionTableName, () => currentTime);

        await expect(service.validateSessionId("test-session-id")).rejects.toMatchObject({
            message: "Session expired",
            statusCode: 403,
        });
    });

    it("throws 400 when client id is missing", async () => {
        send.mockResolvedValue({
            Item: {
                ...validSession,
                clientId: "",
            },
        });

        const service = new SessionService(mockDynamoDbClient, sessionTableName, () => currentTime);

        await expect(service.validateSessionId("test-session-id")).rejects.toMatchObject({
            message: "The Client ID provided for this session is not supported",
            statusCode: 400,
        });
    });

    it("propagates DynamoDB retrieval failures", async () => {
        send.mockRejectedValue(new Error("DynamoDB unavailable"));

        const service = new SessionService(mockDynamoDbClient, sessionTableName, () => currentTime);

        await expect(service.validateSessionId("test-session-id")).rejects.toThrow(
            `Error retrieving ${sessionTableName} item with sessionId: test-session-id`,
        );
    });
});
