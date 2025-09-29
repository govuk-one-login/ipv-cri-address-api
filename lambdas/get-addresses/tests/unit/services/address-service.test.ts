jest.mock("@aws-sdk/lib-dynamodb", () => {
    const originalModule = jest.requireActual("@aws-sdk/lib-dynamodb");
    return {
        __esModule: true,
        ...originalModule,
        GetCommand: jest.fn(),
    };
});

import { DynamoDbClient } from "../../../src/lib/dynamo-db-client";
import { Logger } from "@aws-lambda-powertools/logger";
import { AddressService } from "../../../src/services/address-service";
import { GetCommand } from "@aws-sdk/lib-dynamodb";
import { Address } from "../../../src/types/address";

const mockDynamoDbClient = jest.mocked(DynamoDbClient);
const mockLogger = jest.mocked(Logger);
const mockGetCommand = jest.mocked(GetCommand);

const infoLoggerSpy = jest.spyOn(mockLogger.prototype, "info");
const appendKeysLoggerSpy = jest.spyOn(mockLogger.prototype, "appendKeys").mockImplementation((args) => args);

describe("Address Service Test", () => {
    let addressService: AddressService;
    const addressTableName = "ADDRESS_TABLE";
    const sessionTable = "SESSION_TABLE";
    const sessionId = "SESSION_ID";

    beforeEach(() => {
        jest.resetAllMocks();
        process.env.ADDRESS_TABLE = "ADDRESS_TABLE"
        process.env.SESSION_TABLE = "SESSION_TABLE"

        addressService = new AddressService(DynamoDbClient, mockLogger.prototype);
    });

    describe("with context", () => {
        const sessionItem = { context: "new-context", clientSessionId: "1234567" };

        it("returns an address when passed a session Id", async () => {
            const addressResponse: Address = {
                addresses: [
                    {
                        streetName: "Downing street",
                        buildingNumber: "10",
                        postalCode: "SW1A 2AA",
                        addressRegion: "Greater London Authority",
                    },
                ],
            };

            mockDynamoDbClient.send = jest
                .fn()
                .mockResolvedValueOnce({ Item: sessionItem })
                .mockResolvedValueOnce({ Item: addressResponse });

            const result = await addressService.getAddressesBySessionId(sessionId);

            expect(infoLoggerSpy).toHaveBeenCalledWith("Found session with context: new-context");
            expect(appendKeysLoggerSpy).toHaveBeenCalledWith({ govuk_signin_journey_id: "1234567" });
            expect(mockGetCommand).toHaveBeenNthCalledWith(1, {
                TableName: sessionTable,
                Key: { sessionId },
            });
            expect(mockGetCommand).toHaveBeenNthCalledWith(2, {
                TableName: addressTableName,
                Key: { sessionId },
            });
            expect(result).not.toBeNull();
            expect(result).toEqual({
                context: sessionItem.context,
                addresses: addressResponse.addresses,
            });
        });

        it("returns an empty array when no address is found", async () => {
            mockDynamoDbClient.send = jest
                .fn()
                .mockResolvedValueOnce({ Item: sessionItem })
                .mockResolvedValueOnce({ Item: undefined });

            const result = await addressService.getAddressesBySessionId(sessionId);

            expect(mockGetCommand).toHaveBeenNthCalledWith(1, {
                TableName: sessionTable,
                Key: { sessionId },
            });
            expect(mockGetCommand).toHaveBeenNthCalledWith(2, {
                TableName: addressTableName,
                Key: { sessionId },
            });

            expect(result).not.toBeNull();
            expect(result).toEqual({
                context: sessionItem.context,
                addresses: [],
            });
        });

        it("returns an error when dynamoDB throws an error", async () => {
            try {
                expect.assertions(5);
                mockDynamoDbClient.send = jest
                    .fn()
                    .mockResolvedValueOnce({ Item: sessionItem })
                    .mockRejectedValueOnce(new Error("DynamoDB Error"));

                await addressService.getAddressesBySessionId(sessionId);
            } catch (err) {
                expect(mockGetCommand).toHaveBeenNthCalledWith(1, {
                    TableName: sessionTable,
                    Key: { sessionId },
                });
                expect(mockGetCommand).toHaveBeenNthCalledWith(2, {
                    TableName: addressTableName,
                    Key: { sessionId },
                });

                expect(err).toBeDefined();
                expect(typeof err).toBe("object");

                const errorObj = err as Error;
                expect(errorObj.message).toContain("DynamoDB Error");
            }
        });
    });

    describe("without context", () => {
        const sessionItem = undefined;

        it("returns an address when passed a session Id", async () => {
            const addressResponse: Address = {
                addresses: [
                    {
                        streetName: "Downing street",
                        buildingNumber: "10",
                        postalCode: "SW1A 2AA",
                    },
                ],
            };

            mockDynamoDbClient.send = jest
                .fn()
                .mockResolvedValueOnce({ Item: sessionItem })
                .mockResolvedValueOnce({ Item: addressResponse });

            const result = await addressService.getAddressesBySessionId(sessionId);

            expect(mockGetCommand).toHaveBeenNthCalledWith(1, {
                TableName: sessionTable,
                Key: { sessionId },
            });
            expect(mockGetCommand).toHaveBeenNthCalledWith(2, {
                TableName: addressTableName,
                Key: { sessionId },
            });
            expect(result).not.toBeNull();
            expect(result).toEqual({
                context: undefined,
                addresses: addressResponse.addresses,
            });
        });

        it("returns an empty array when no address is found", async () => {
            mockDynamoDbClient.send = jest
                .fn()
                .mockResolvedValueOnce({ Item: sessionItem })
                .mockResolvedValueOnce({ Item: undefined });

            const result = await addressService.getAddressesBySessionId(sessionId);

            expect(mockGetCommand).toHaveBeenNthCalledWith(1, {
                TableName: sessionTable,
                Key: { sessionId },
            });
            expect(mockGetCommand).toHaveBeenNthCalledWith(2, {
                TableName: addressTableName,
                Key: { sessionId },
            });

            expect(result).not.toBeNull();
            expect(result).toEqual({
                addresses: [],
            });
        });

        it("returns an error when dynamoDB throws an error", async () => {
            try {
                expect.assertions(5);
                mockDynamoDbClient.send = jest
                    .fn()
                    .mockResolvedValueOnce({ Item: sessionItem })
                    .mockRejectedValueOnce(new Error("DynamoDB Error"));

                await addressService.getAddressesBySessionId(sessionId);
            } catch (err) {
                expect(mockGetCommand).toHaveBeenNthCalledWith(1, {
                    TableName: sessionTable,
                    Key: { sessionId },
                });
                expect(mockGetCommand).toHaveBeenNthCalledWith(2, {
                    TableName: addressTableName,
                    Key: { sessionId },
                });

                expect(err).toBeDefined();
                expect(typeof err).toBe("object");

                const errorObj = err as Error;
                expect(errorObj.message).toContain("DynamoDB Error");
            }
        });
    });
});
