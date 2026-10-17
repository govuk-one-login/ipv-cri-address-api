import { APIGatewayProxyEventHeaders } from "aws-lambda";

import { CriError } from "@govuk-one-login/cri-error-response";

export const getSessionId = (headers: APIGatewayProxyEventHeaders) => {
    const sessionId = headers["session_id"];

    if (!sessionId?.trim()) {
        throw new CriError(403, "Session not found");
    }

    return sessionId;
};
