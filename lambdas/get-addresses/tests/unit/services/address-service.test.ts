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
import { CanonicalAddress } from "../../../src/types/canonical-address";
import { AddressService } from "../../../src/services/address-service";
import { GetCommand } from "@aws-sdk/lib-dynamodb";
import { DynamoDbClient } from "../../../src/lib/dynamo-db-client";

const mockDynamoDbClient = jest.mocked(DynamoDbClient);
const mockGetCommand = jest.mocked(GetCommand);
const getParameterMock = jest.mocked(getParameter);

describe("Address Service", () => {
    let addressService: AddressService;
    const tableName = "MY_TABLE";
    const sessionId = "SESSION_ID";

    beforeEach(() => {
        jest.resetAllMocks();
        getParameterMock.mockResolvedValue(tableName);

        addressService = new AddressService(tableName, DynamoDbClient);
    });

    it("Should return an address when passed a session Id", async () => {
        const addresses: CanonicalAddress[] = [
            {
                streetName: "Downing street",
                buildingNumber: "10",
                postalCode: "SW1A 2AA",
            },
        ];

        mockDynamoDbClient.send = jest.fn().mockResolvedValue({ Item: addresses });

        const result = await addressService.getAddressesBySessionId(sessionId);
        expect(mockGetCommand).toHaveBeenCalledWith({
            TableName: tableName,
            Key: {
                sessionId,
            },
        });
        expect(result).not.toBeNull();
        expect(result).toEqual(addresses);
    });

    it("Should return an empty array when no address is found", async () => {
        mockDynamoDbClient.send = jest.fn().mockResolvedValue({ Item: undefined });

        const result = await addressService.getAddressesBySessionId(sessionId);
        expect(mockGetCommand).toHaveBeenCalledWith({
            TableName: tableName,
            Key: {
                sessionId,
            },
        });
        expect(result).not.toBeNull();
        expect(result).toEqual([]);
    });

    it("Should return an error when dynamoDB throws an error", async () => {
        try {
            expect.assertions(4);
            mockDynamoDbClient.send = jest.fn().mockRejectedValue(new Error("DynamoDB Error"));

            await addressService.getAddressesBySessionId(sessionId);
        } catch (err) {
            expect(mockGetCommand).toHaveBeenCalledWith({
                TableName: tableName,
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
