jest.mock("@aws-sdk/lib-dynamodb", () => {
    const originalModule = jest.requireActual("@aws-sdk/lib-dynamodb");
    return {
        __esModule: true,
        ...originalModule,
        GetCommand: jest.fn(),
    };
});
jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
    getParameter: jest.fn(),
}));

import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { DynamoDbClient } from "../../../src/lib/dynamo-db-client";
import { Logger } from "@aws-lambda-powertools/logger";
import { CanonicalAddress } from "../../../src/types/canonical-address";
import { AddressServiceV2 } from "../../../src/v2/services/address-service-v2";
import { GetCommand } from "@aws-sdk/lib-dynamodb";
const mockDynamoDbClient = jest.mocked(DynamoDbClient);
const mockLogger = jest.mocked(Logger);
const mockGetCommand = jest.mocked(GetCommand);
const getParameterMock = jest.mocked(getParameter);

describe("Address Service V2 Test", () => {
    let addressService: AddressServiceV2;
    const addressTableName = "ADDRESS_TABLE";
    const sessionTable = "SESSION_TABLE";
    const sessionId = "SESSION_ID";

    beforeEach(() => {
        jest.resetAllMocks();
        getParameterMock.mockResolvedValueOnce(addressTableName);
        getParameterMock.mockResolvedValueOnce(sessionTable);

        addressService = new AddressServiceV2(DynamoDbClient, mockLogger.prototype);
    });

    const testCases = [
        {
            description: "with context",
            sessionItem: { context: "new-context" },
            expectedResult: (addresses: CanonicalAddress[]) => ({
                context: "new-context",
                addresses,
            }),
        },
        {
            description: "without context",
            sessionItem: undefined,
            expectedResult: (addresses: CanonicalAddress[]) => ({
                addresses,
            }),
        },
    ];

    testCases.forEach(({ description, sessionItem, expectedResult }) => {
        describe(description, () => {
            it("returns an address when passed a session Id", async () => {
                const addresses: CanonicalAddress[] = [
                    {
                        streetName: "Downing street",
                        buildingNumber: "10",
                        postalCode: "SW1A 2AA",
                    },
                ];

                mockDynamoDbClient.send = jest
                    .fn()
                    .mockResolvedValueOnce({ Item: sessionItem })
                    .mockResolvedValueOnce({ Item: addresses });

                const result = await addressService.getAddressesBySessionId(sessionId);
                expect(mockGetCommand).toHaveBeenNthCalledWith(1, {
                    TableName: sessionTable,
                    Key: {
                        sessionId,
                    },
                });
                expect(mockGetCommand).toHaveBeenNthCalledWith(2, {
                    TableName: addressTableName,
                    Key: {
                        sessionId,
                    },
                });

                expect(result).not.toBeNull();
                expect(result).toEqual(expectedResult(addresses));
            });

            it("returns an empty array when no address is found", async () => {
                mockDynamoDbClient.send = jest
                    .fn()
                    .mockResolvedValueOnce({ Item: sessionItem })
                    .mockResolvedValueOnce({ Item: undefined });

                const result = await addressService.getAddressesBySessionId(sessionId);

                expect(mockGetCommand).toHaveBeenNthCalledWith(1, {
                    TableName: sessionTable,
                    Key: {
                        sessionId,
                    },
                });
                expect(mockGetCommand).toHaveBeenNthCalledWith(2, {
                    TableName: addressTableName,
                    Key: {
                        sessionId,
                    },
                });

                expect(result).not.toBeNull();
                expect(result).toEqual(expectedResult([]));
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
                        Key: {
                            sessionId,
                        },
                    });
                    expect(mockGetCommand).toHaveBeenNthCalledWith(2, {
                        TableName: addressTableName,
                        Key: {
                            sessionId,
                        },
                    });

                    expect(err).toBeDefined();
                    expect(typeof err).toBe("object");

                    const errorObj = err as Error;
                    expect(errorObj.message).toContain("DynamoDB Error");
                }
            });
        });
    });
});
