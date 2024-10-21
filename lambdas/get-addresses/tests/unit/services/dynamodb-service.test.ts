import { Logger } from "@aws-lambda-powertools/logger";
import { DynamoDbService } from "../../../src/services/dynamodb-service";
import { DynamoDBDocument } from "@aws-sdk/lib-dynamodb";

const mockDynamoDbClient = {
    send: jest.fn(),
};
const mockLogger = {
    error: jest.fn(),
    warn: jest.fn(),
} as unknown as Logger;

describe("DynamoDbService", () => {
    let dynamoDbService: DynamoDbService;

    beforeEach(() => {
        dynamoDbService = new DynamoDbService(mockDynamoDbClient as unknown as DynamoDBDocument, mockLogger);
        jest.clearAllMocks();
    });

    const sessionId = "session-id";
    const tableName = "test-table";

    it("returns the logger instance", () => {
        const logger = dynamoDbService.getLogger();

        expect(logger).toBe(mockLogger);
    });

    it("returns the item when found", async () => {
        const mockItem = { sessionId: "test-session-id", data: "some data" };
        mockDynamoDbClient.send.mockResolvedValueOnce({ Item: mockItem });

        const result = await dynamoDbService.getItem(sessionId, tableName);

        expect(mockDynamoDbClient.send).toHaveBeenCalledWith(
            expect.objectContaining({
                input: {
                    TableName: tableName,
                    Key: { sessionId: sessionId },
                },
            }),
        );

        expect(mockLogger.warn).not.toHaveBeenCalled();
        expect(result).toEqual(mockItem);
    });

    it("logs a warning when no item is found", async () => {
        mockDynamoDbClient.send.mockResolvedValueOnce({ Item: undefined });

        const result = await dynamoDbService.getItem(sessionId, tableName);

        expect(mockLogger.warn).toHaveBeenCalledWith(`Could not find ${tableName} item with id: ${sessionId}`);

        expect(result).toBeUndefined();
    });

    it("logs an error and throw when DynamoDB throws an error", async () => {
        const mockError = new Error("DynamoDB Error");
        mockDynamoDbClient.send.mockRejectedValueOnce(mockError);

        await expect(dynamoDbService.getItem(sessionId, tableName)).rejects.toThrow(
            `Error retrieving ${tableName} item with sessionId: ${sessionId}, due to ${mockError}`,
        );

        expect(mockLogger.error).toHaveBeenCalledWith(
            `Error fetching item from ${tableName} for sessionId: ${sessionId}`,
            mockError,
        );
    });
});
