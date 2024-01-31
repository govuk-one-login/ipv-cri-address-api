jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
    getParameter: jest.fn(),
}));
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { APIGatewayProxyEvent } from "aws-lambda";
import { lambdaHandler } from "../../src/app";
import { CanonicalAddress } from "../../src/types/address";
import { DynamoDbClient } from "../../src/lib/dynamo-db-client";

const mockDynamoDbClient = jest.mocked(DynamoDbClient);
const mockedGetParameter = getParameter as jest.MockedFunction<typeof getParameter>;

describe("Handler", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedGetParameter.mockResolvedValue("Address-cri-api/AddressTableName");
    });

    it("should return an address when an address is found", async () => {
        const addresses: CanonicalAddress[] = [
            {
                streetName: "Downing street",
                buildingNumber: "10",
                postalCode: "SW1A 2AA",
            },
        ];

        const savedAddress = { Item: addresses };

        mockDynamoDbClient.send = jest.fn().mockResolvedValue(savedAddress);

        const params = {
            headers: {
                session_id: "123-abc",
            },
        } as unknown as APIGatewayProxyEvent;

        const result = await lambdaHandler(params);
        const resultAddress = JSON.parse(result.body).result;
        expect(resultAddress).toEqual(addresses);
        expect(result.statusCode).toBe(200);
    });

    it("should return empty array when no address is found", async () => {
        mockDynamoDbClient.send = jest.fn().mockResolvedValue({ Item: undefined });

        const params = {
            headers: {
                session_id: "123-abc",
            },
        } as unknown as APIGatewayProxyEvent;
        const result = await lambdaHandler(params);
        const resultAddress = JSON.parse(result.body).result;
        expect(resultAddress).toEqual([]);
        expect(result.statusCode).toBe(200);
    });

    it("should return a status code 400 when no sesson id is passed", async () => {
        const params = {
            headers: {},
        } as unknown as APIGatewayProxyEvent;

        const result = await lambdaHandler(params);
        const errorMessage = result.body;
        expect(errorMessage).toContain("session_id is required");
        expect(result.statusCode).toBe(400);
    });

    it("should return a status code 500 when we cannot connect to dynamodb", async () => {
        mockDynamoDbClient.send = jest.fn().mockRejectedValue(new Error("DynamoDB Error"));

        const params = {
            headers: {
                session_id: "123-abc",
            },
        } as unknown as APIGatewayProxyEvent;
        const result = await lambdaHandler(params);
        expect(result.statusCode).toBe(500);
        expect(result.body).toContain("DynamoDB Error");
    });
});
