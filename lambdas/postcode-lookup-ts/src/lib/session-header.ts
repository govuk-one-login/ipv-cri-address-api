import { APIGatewayProxyEventHeaders } from "aws-lambda";

import { CriError } from "@govuk-one-login/cri-error-response";

export const getSessionId = (headers: APIGatewayProxyEventHeaders) => {
    const sessionId = headers["session_id"];

    if (!sessionId) {
        throw new CriError(400, "Missing header: session_id is required");
    }

    return sessionId;
};
