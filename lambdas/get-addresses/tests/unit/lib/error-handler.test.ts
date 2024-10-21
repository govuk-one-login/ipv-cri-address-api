import { Logger } from "@aws-lambda-powertools/logger";
import { handleError } from "../../../src/lib/error-handler";
import { APIGatewayProxyResult } from "aws-lambda";

const mockLogger = {
    error: jest.fn(),
} as unknown as Logger;

describe("handleError", () => {
    const functionName = "testFunction";

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it("handles Error objects and log the error without statusCode set and default to 500", () => {
        const mockError = new Error("Test error message");

        const result = handleError(mockLogger, mockError, functionName);

        expect(mockLogger.error).toHaveBeenCalledWith(`Error in ${functionName}: ${mockError.message}`, {
            stack: mockError.stack,
        });

        expect(result).toEqual({
            statusCode: 500,
            body: JSON.stringify({ message: "Test error message" }),
        });
    });

    it("handles Error objects and log the error with statusCode set", () => {
        const mockError = new Error("Test error message");
        (mockError as unknown as APIGatewayProxyResult).statusCode = 400;
        (mockError as unknown as APIGatewayProxyResult).body = "Missing header: session_id is required";

        const result = handleError(mockLogger, mockError, functionName);

        expect(mockLogger.error).toHaveBeenCalledWith(`Error in ${functionName}: ${mockError.message}`, {
            stack: mockError.stack,
        });

        expect(result).toEqual({
            statusCode: 400,
            body: JSON.stringify({ message: "Missing header: session_id is required" }),
        });
    });

    it("handles API Gateway-style error responses and log the error", () => {
        const apiError: APIGatewayProxyResult = {
            statusCode: 404,
            body: "Not Found",
        };

        const result = handleError(mockLogger, apiError, functionName);

        expect(mockLogger.error).toHaveBeenCalledWith(`Error in ${functionName}: ${JSON.stringify("Not Found")}`);

        expect(result).toEqual({
            statusCode: 404,
            body: JSON.stringify({ message: "Not Found" }),
        });
    });

    it("handles unknown error types and log a generic error", () => {
        const unknownError = 12345;

        const result = handleError(mockLogger, unknownError, functionName);

        expect(mockLogger.error).toHaveBeenCalledWith(`Unknown error in ${functionName}: ${String(unknownError)}`);

        expect(result).toEqual({
            statusCode: 500,
            body: JSON.stringify({ message: "Internal Server Error" }),
        });
    });

    it("handles null or undefined error and log a generic error", () => {
        const nullError = null;

        const result = handleError(mockLogger, nullError, functionName);

        expect(mockLogger.error).toHaveBeenCalledWith(`Unknown error in ${functionName}: null`);

        expect(result).toEqual({
            statusCode: 500,
            body: JSON.stringify({ message: "Internal Server Error" }),
        });
    });
});
