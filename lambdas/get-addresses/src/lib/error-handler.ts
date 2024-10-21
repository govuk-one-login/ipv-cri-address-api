import { Logger } from "@aws-lambda-powertools/logger";
export interface ErrorResponse {
    statusCode: number;
    body: string;
}

export class ApiError extends Error {
    constructor(
        public message: string = "Internal Server Error",
        public statusCode: number = 500,
    ) {
        super(message);
        this.name = "ApiError";
    }
}

export const handleError = (logger: Logger, error: unknown, loggerMessage: string): ErrorResponse => {
    let statusCode = 500;
    let message = "Internal Server Error";

    if (error instanceof ApiError || error instanceof Error) {
        statusCode = (error as ApiError).statusCode || 500;
        message = error.message;
    }

    const logDetails = error instanceof Error ? { stack: error.stack } : {};
    logger.error(`${loggerMessage}: ${message}`, logDetails);
    return {
        statusCode,
        body: JSON.stringify({ message }),
    };
};
export class BadRequestError extends ApiError {
    constructor(message: string = "Bad Request") {
        super(message, 400);
        this.name = "BadRequestError";
    }
}
