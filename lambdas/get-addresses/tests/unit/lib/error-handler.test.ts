import { Logger } from "@aws-lambda-powertools/logger";
import { ApiError, BadRequestError, handleError } from "../../../src/lib/error-handler";

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

        const result = handleError(mockLogger, mockError, `Error in ${functionName}`);

        expect(mockLogger.error).toHaveBeenCalledWith(`Error in ${functionName}: ${mockError.message}`, {
            stack: mockError.stack,
        });

        expect(result).toEqual({
            statusCode: 500,
            body: JSON.stringify({ message: "Test error message" }),
        });
    });

    it("handles Error objects and log the error with statusCode set", () => {
        const mockError = new BadRequestError("Missing header: session_id is required");

        const result = handleError(mockLogger, mockError, `Error in ${functionName}`);

        expect(mockLogger.error).toHaveBeenCalledWith(`Error in ${functionName}: ${mockError.message}`, {
            stack: mockError.stack,
        });

        expect(result).toEqual({
            statusCode: 400,
            body: JSON.stringify({ message: "Missing header: session_id is required" }),
        });
    });

    it("handles API Gateway-style error responses and log the error", () => {
        const apiError = new ApiError("Not Found", 404);

        const result = handleError(mockLogger, apiError, `Error in ${functionName}`);

        expect(mockLogger.error).toHaveBeenCalledWith(`Error in ${functionName}: Not Found`, {
            stack: apiError.stack,
        });

        expect(result).toEqual({
            statusCode: 404,
            body: JSON.stringify({ message: "Not Found" }),
        });
    });

    it("handles unknown error types and log a generic error", () => {
        const unknownError = new ApiError();

        const result = handleError(mockLogger, unknownError, `Unknown error in ${functionName}`);

        expect(mockLogger.error).toHaveBeenCalledWith(
            `Unknown error in ${functionName}: ${String(unknownError.message)}`,
            {
                stack: unknownError.stack,
            },
        );

        expect(result).toEqual({
            statusCode: 500,
            body: JSON.stringify({ message: "Internal Server Error" }),
        });
    });

    it("handles null or undefined error and log a generic error", () => {
        const nullError = null;

        const result = handleError(mockLogger, nullError, `Unknown error in ${functionName}`);

        expect(mockLogger.error).toHaveBeenCalledWith(`Unknown error in ${functionName}: Internal Server Error`, {});

        expect(result).toEqual({
            statusCode: 500,
            body: JSON.stringify({ message: "Internal Server Error" }),
        });
    });
});
