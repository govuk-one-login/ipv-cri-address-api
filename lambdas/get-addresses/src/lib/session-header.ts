import { APIGatewayProxyEventHeaders } from "aws-lambda";
import { ApiError } from "./error-handler";

export const getSessionId = (headers: APIGatewayProxyEventHeaders) => {
    const sessionId = headers["session_id"];
    if (!sessionId) {
        throw new ApiError("Missing header: session_id is required", 400);
    }
    return sessionId;
};
