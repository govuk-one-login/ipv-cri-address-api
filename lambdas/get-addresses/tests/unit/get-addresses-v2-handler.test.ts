import { Logger } from "@aws-lambda-powertools/logger";
import { AddressesV2Handler } from "../../src/v2/get-addresses-v2-handler";
import { APIGatewayProxyEvent, Context } from "aws-lambda";
import { DynamoDbClient } from "../../src/lib/dynamo-db-client";
import { AddressServiceV2 } from "../../src/v2/services/address-service-v2";
import { BadRequestError } from "../../src/lib/error-handler";
jest.mock("@aws-lambda-powertools/logger");
jest.mock("../../src/v2/services/address-service-v2");
const logger = new Logger();
const mockedLogger = jest.mocked(Logger);
const dynamoDbClientMock = jest.mocked(DynamoDbClient);
const addressService = new AddressServiceV2(dynamoDbClientMock, logger);
const mockContext: Partial<Context> = {
    functionName: "testFunction",
};

describe("get-addresses-v2-handler", () => {
    let loggerSpy: jest.SpyInstance<unknown, never, unknown>;
    const handlerClass = new AddressesV2Handler(addressService);

    beforeEach(() => {
        jest.clearAllMocks();

        loggerSpy = jest.spyOn(mockedLogger.prototype, "error");
    });

    it("returns 400 when session_id header is missing", async () => {
        const result = await handlerClass.handler(
            {
                headers: {},
            } as APIGatewayProxyEvent,
            mockContext as Context,
        );

        expect(result).toEqual({
            body: JSON.stringify({ message: "Missing header: session_id is required" }),
            statusCode: 400,
        });
    });

    it("handles standard Error thrown by service and returns status code 500", async () => {
        const params: Partial<APIGatewayProxyEvent> = {
            headers: {
                session_id: "123-abc",
            },
        };
        const dynamoError = new Error("DynamoDB Error");

        jest.spyOn(addressService, "getAddressesBySessionId").mockRejectedValueOnce(dynamoError);
        const response = await handlerClass.handler(params as APIGatewayProxyEvent, mockContext as Context);

        expect(response).toEqual({
            statusCode: 500,
            body: JSON.stringify({ message: "DynamoDB Error" }),
        });
        expect(loggerSpy).toHaveBeenCalledWith(`Error in testFunction: ${dynamoError.message}`, {
            stack: dynamoError.stack,
        });
    });

    it("handles non-standard error with custom status code and message", async () => {
        const params: Partial<APIGatewayProxyEvent> = {
            headers: {
                session_id: "123-abc",
            },
        };

        const customError: BadRequestError = new BadRequestError("Custom Error Message");
        jest.spyOn(addressService, "getAddressesBySessionId").mockRejectedValueOnce(customError);

        const response = await handlerClass.handler(params as APIGatewayProxyEvent, mockContext as Context);

        expect(response).toEqual({
            statusCode: 400,
            body: JSON.stringify({ message: "Custom Error Message" }),
        });
        expect(loggerSpy).toHaveBeenCalledWith("Error in testFunction: Custom Error Message", {
            stack: customError.stack,
        });
    });

    it("handles an unknown error and returns default 500 Internal Server Error", async () => {
        const params: Partial<APIGatewayProxyEvent> = {
            headers: {
                session_id: "123-abc",
            },
        };

        const unknownError = new Error("Internal Server Error");
        jest.spyOn(addressService, "getAddressesBySessionId").mockRejectedValueOnce(unknownError);

        const response = await handlerClass.handler(params as APIGatewayProxyEvent, mockContext as Context);

        expect(response).toEqual({
            statusCode: 500,
            body: JSON.stringify({ message: "Internal Server Error" }),
        });
        expect(loggerSpy).toHaveBeenCalledWith("Error in testFunction: Internal Server Error", {
            stack: unknownError.stack,
        });
    });
    describe("with context", () => {
        it("returns 200 with context when getAddressesBySessionId resolves successfully", async () => {
            const mockResult = {
                context: "new-context",
                addresses: [
                    {
                        streetName: "Downing street",
                        buildingNumber: "10",
                        postalCode: "SW1A 2AA",
                    },
                ],
            };

            const mockEvent: Partial<APIGatewayProxyEvent> = {
                headers: {
                    session_id: "test-session-id",
                },
            };

            jest.spyOn(addressService, "getAddressesBySessionId").mockResolvedValueOnce(mockResult);
            const response = await handlerClass.handler(mockEvent as APIGatewayProxyEvent, mockContext as Context);

            expect(response).toEqual({
                statusCode: 200,
                body: JSON.stringify({ result: mockResult }),
            });
            expect(addressService.getAddressesBySessionId).toHaveBeenCalledWith("test-session-id");
        });

        it("returns 200 with an empty address array when no address is found", async () => {
            const mockResult = {
                context: "new-context",
                addresses: [],
            };

            const mockEvent: Partial<APIGatewayProxyEvent> = {
                headers: {
                    session_id: "test-session-id",
                },
            };

            jest.spyOn(addressService, "getAddressesBySessionId").mockResolvedValueOnce(mockResult);
            const response = await handlerClass.handler(mockEvent as APIGatewayProxyEvent, mockContext as Context);

            expect(response).toEqual({
                statusCode: 200,
                body: JSON.stringify({ result: mockResult }),
            });
            expect(addressService.getAddressesBySessionId).toHaveBeenCalledWith("test-session-id");
        });
    });

    describe("without context", () => {
        it("returns 200 without context when getAddressesBySessionId resolves successfully", async () => {
            const mockResult = {
                addresses: [
                    {
                        streetName: "Downing street",
                        buildingNumber: "10",
                        postalCode: "SW1A 2AA",
                    },
                ],
            };

            const mockEvent: Partial<APIGatewayProxyEvent> = {
                headers: {
                    session_id: "test-session-id",
                },
            };

            jest.spyOn(addressService, "getAddressesBySessionId").mockResolvedValueOnce(mockResult);
            const response = await handlerClass.handler(mockEvent as APIGatewayProxyEvent, mockContext as Context);

            expect(response).toEqual({
                statusCode: 200,
                body: JSON.stringify({ result: mockResult }),
            });
            expect(addressService.getAddressesBySessionId).toHaveBeenCalledWith("test-session-id");
        });

        it("returns 200 with an empty address array and no context when no address is found", async () => {
            const mockResult = {
                addresses: [],
            };

            const mockEvent: Partial<APIGatewayProxyEvent> = {
                headers: {
                    session_id: "test-session-id",
                },
            };

            jest.spyOn(addressService, "getAddressesBySessionId").mockResolvedValueOnce(mockResult);
            const response = await handlerClass.handler(mockEvent as APIGatewayProxyEvent, mockContext as Context);

            expect(response).toEqual({
                statusCode: 200,
                body: JSON.stringify({ result: mockResult }),
            });
            expect(addressService.getAddressesBySessionId).toHaveBeenCalledWith("test-session-id");
        });
    });
});
