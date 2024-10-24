import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { APIGatewayProxyEvent } from "aws-lambda";
import { lambdaHandler } from "../../src/app";
import { DynamoDbClient } from "../../src/lib/dynamo-db-client";
import { CanonicalAddress } from "../../src/types/canonical-address";
import { Address } from "../../src/types/address";

jest.mock("@aws-lambda-powertools/parameters/ssm", () => ({
    getParameter: jest.fn(),
}));

const dynamoDbClientMock = jest.mocked(DynamoDbClient);
const getParameterMock = jest.mocked(getParameter);

describe("Handler", () => {
    beforeEach(() => {
        jest.clearAllMocks();

        getParameterMock.mockResolvedValue("Address-cri-api/AddressTableName");
    });

    it("should return an address when an address is found", async () => {
        const addressResponse: Address = {
            addresses: [
                {
                    streetName: "Downing street",
                    buildingNumber: "10",
                    postalCode: "SW1A 2AA",
                },
            ],
        };

        const addresses: CanonicalAddress[] = addressResponse.addresses;
        const savedAddress = { Item: addressResponse };

        dynamoDbClientMock.send = jest.fn().mockResolvedValue(savedAddress);

        const params = {
            headers: {
                session_id: "123-abc",
            },
        } as unknown as APIGatewayProxyEvent;

        const result = await lambdaHandler(params);
        const resultAddress = JSON.parse(result.body);
        expect(resultAddress).toEqual(addresses);
        expect(result.statusCode).toBe(200);
    });

    it("should return empty array when no address is found", async () => {
        dynamoDbClientMock.send = jest.fn().mockResolvedValue({ Item: undefined });

        const params = {
            headers: {
                session_id: "123-abc",
            },
        } as unknown as APIGatewayProxyEvent;
        const result = await lambdaHandler(params);
        const resultAddress = JSON.parse(result.body);
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
        dynamoDbClientMock.send = jest.fn().mockRejectedValue(new Error("DynamoDB Error"));

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
