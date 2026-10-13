import type { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";

import { buildAndSendAuditEvent } from "@govuk-one-login/cri-audit";
import { CriError, formatErrorResponse } from "@govuk-one-login/cri-error-response";
import { logger } from "@govuk-one-login/cri-logger";
import { captureMetric, captureMetricWithDimensions } from "@govuk-one-login/cri-metrics";
import type { SessionItem } from "@govuk-one-login/cri-types";

import { getPostcodeErrorDimensions, POSTCODE_LOOKUP_ERROR } from "./lib/postcode-error-metrics";
import { getSessionId } from "./lib/session-header";
import type { CanonicalAddress } from "./types/canonical-address";
import type { PostcodeRequest } from "./types/postcode-request";

const AUDIT_EVENT_TYPE = {
    REQUEST_SENT: "REQUEST_SENT",
    RESPONSE_RECEIVED: "RESPONSE_RECEIVED",
} as const;

const POSTCODE_LOOKUP_METRIC = "postcode_lookup";
const TXMA_AUDIT_ENCODED_HEADER = "txma-audit-encoded";

export interface HandlerDependencies {
    queueUrl: string;
    componentId: string;
    validateSessionId: (sessionId: string) => Promise<SessionItem>;
    lookupPostcode: (postcode: string, clientId: string) => Promise<CanonicalAddress[]>;
}

export const createHandler =
    ({ queueUrl, componentId, validateSessionId, lookupPostcode }: HandlerDependencies) =>
    async (event: APIGatewayProxyEvent, _context: Context): Promise<APIGatewayProxyResult> => {
        try {
            const postcode = getPostcodeFromRequest(event);
            const sessionId = getSessionId(event.headers);
            const session = await validateSessionId(sessionId);
            logger.info("found session");
            const restricted = buildAuditRestricted(postcode, event.headers);

            await buildAndSendAuditEvent(queueUrl, AUDIT_EVENT_TYPE.REQUEST_SENT, componentId, session, { restricted });

            const results = await lookupPostcode(postcode, session.clientId);

            captureMetric(POSTCODE_LOOKUP_METRIC);

            await buildAndSendAuditEvent(queueUrl, AUDIT_EVENT_TYPE.RESPONSE_RECEIVED, componentId, session, {
                restricted,
            });

            return {
                statusCode: 200,
                body: JSON.stringify(results),
            };
        } catch (error: unknown) {
            captureMetricWithDimensions(POSTCODE_LOOKUP_ERROR, getPostcodeErrorDimensions(error));

            const errorResponse = formatErrorResponse(error);

            if (
                error instanceof CriError &&
                error.statusCode === 400 &&
                (error.message === "Missing postcode in request body." ||
                    error.message === "Failed to parse postcode from request body")
            ) {
                return {
                    ...errorResponse,
                    body: JSON.stringify(error.message),
                };
            }

            if (error instanceof CriError && error.statusCode === 404) {
                return {
                    ...errorResponse,
                    body: JSON.stringify(error.message),
                };
            }

            if (error instanceof SyntaxError) {
                return {
                    ...errorResponse,
                    statusCode: 401,
                    body: JSON.stringify(error.message),
                };
            }

            return errorResponse;
        }
    };

const getPostcodeFromRequest = (event: APIGatewayProxyEvent): string => {
    if (!event.body) {
        throw new CriError(400, "Missing postcode in request body.");
    }

    let request: PostcodeRequest;

    try {
        request = JSON.parse(event.body) as PostcodeRequest;
    } catch {
        throw new CriError(400, "Failed to parse postcode from request body");
    }

    if (!request.postcode?.trim()) {
        throw new CriError(400, "Missing postcode in request body.");
    }

    return request.postcode;
};

const buildAuditRestricted = (postcode: string, headers: APIGatewayProxyEvent["headers"]): Record<string, unknown> => {
    const encodedDeviceInformation = headers[TXMA_AUDIT_ENCODED_HEADER];

    return {
        addresses: [
            {
                postalCode: decodeURIComponent(postcode).toUpperCase(),
            },
        ],
        ...(encodedDeviceInformation && {
            device_information: {
                encoded: encodedDeviceInformation,
            },
        }),
    };
};
