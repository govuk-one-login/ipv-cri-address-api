import { APIGatewayProxyEventHeaders, APIGatewayProxyResult } from "aws-lambda";

export const getSessionId = (headers: APIGatewayProxyEventHeaders) => {
    const sessionId = headers["session_id"];
    if (!sessionId) {
        const error = new Error("Missing header: session_id is required");
        (error as unknown as APIGatewayProxyResult).statusCode = 400;
        (error as unknown as APIGatewayProxyResult).body = "Missing header: session_id is required";

        throw error;
    }
    return sessionId;
};
