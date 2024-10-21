import { Logger } from "@aws-lambda-powertools/logger";
import { APIGatewayProxyResult } from "aws-lambda";

export const handleError = (logger: Logger, error: unknown, functionName: string): APIGatewayProxyResult => {
    let statusCode = 500;
    let body = "Internal Server Error";

    if (error instanceof Error) {
        const { message, stack } = error;
        body = (error as unknown as APIGatewayProxyResult).body || message;
        statusCode = (error as unknown as APIGatewayProxyResult).statusCode || 500;
        logger.error(`Error in ${functionName}: ${message}`, { stack });
    } else if (typeof error === "object" && error !== null) {
        const apiError = error as APIGatewayProxyResult;
        statusCode = apiError.statusCode || 500;
        body = apiError.body || body;
        logger.error(`Error in ${functionName}: ${JSON.stringify(body)}`);
    } else {
        logger.error(`Unknown error in ${functionName}: ${String(error)}`);
    }

    return {
        statusCode,
        body: JSON.stringify({ message: body }),
    };
};
