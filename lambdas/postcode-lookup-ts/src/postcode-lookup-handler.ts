import type { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";

import { buildAndSendAuditEvent } from "@govuk-one-login/cri-audit";
import { CriError, formatErrorResponse } from "@govuk-one-login/cri-error-response";
import { logger } from "@govuk-one-login/cri-logger";
import { captureMetric, captureMetricWithDimensions } from "@govuk-one-login/cri-metrics";
import type { SessionItem } from "@govuk-one-login/cri-types";

import { decodePostcode } from "./lib/postcode.js";
import { getPostcodeErrorDimensions, POSTCODE_LOOKUP_ERROR } from "./lib/postcode-error-metrics.js";
import { getSessionId } from "./lib/session-header.js";
import { sanitize } from "./lib/pii-postcode-masker.js";
import type { CanonicalAddress } from "./types/canonical-address.js";
import type { PostcodeRequest } from "./types/postcode-request.js";

const POSTCODE_LOOKUP_METRIC = "postcode_lookup";
const TXMA_AUDIT_ENCODED_HEADER = "txma-audit-encoded";
const CLIENT_IP_HEADER = "X-Forwarded-For";

interface HandlerDependencies {
    queueUrl: string;
    componentId: string;
    auditEventPrefix: string;
    getSessionById: (sessionId: string) => Promise<SessionItem>;
    lookupPostcode: (postcode: string, clientId: string) => Promise<CanonicalAddress[]>;
}

export const createHandler =
    ({ queueUrl, componentId, auditEventPrefix, getSessionById, lookupPostcode }: HandlerDependencies) =>
    async (event: APIGatewayProxyEvent, _context: Context): Promise<APIGatewayProxyResult> => {
        try {
            logger.info("Postcode lookup request received");
            const postcode = getPostcodeFromRequest(event);
            const sessionId = getSessionId(event.headers);
            logger.info("Retrieving session");
            const session = await getSessionById(sessionId);

            logger.info("found session");
            validatePostcode(postcode);
            logger.info("Postcode request validated");
            const restricted = buildAuditRestricted(postcode, event.headers);
            const auditSession = buildAuditSession(session, event.headers);
            await buildAndSendAuditEvent(queueUrl, `${auditEventPrefix}_REQUEST_SENT`, componentId, auditSession, {
                restricted,
            });
            logger.info("REQUEST_SENT audit event sent");

            logger.info("Calling postcode lookup service");
            const results = await lookupPostcode(postcode, session.clientId);
            logger.info("Postcode lookup service call completed");
            captureMetric(POSTCODE_LOOKUP_METRIC);

            await buildAndSendAuditEvent(queueUrl, `${auditEventPrefix}_RESPONSE_RECEIVED`, componentId, auditSession, {
                restricted,
            });
            logger.info("RESPONSE_RECEIVED audit event sent");

            logger.info("Postcode lookup completed successfully");

            return {
                statusCode: 200,
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(results),
            };
        } catch (error: unknown) {
            const errorToLog =
                error instanceof Error
                    ? new Error(sanitize(error.message) ?? "Postcode lookup failed")
                    : new Error("Unknown postcode lookup error");

            logger.error("Postcode lookup failed", errorToLog);

            captureMetricWithDimensions(POSTCODE_LOOKUP_ERROR, getPostcodeErrorDimensions(error));

            return buildErrorResponse(error);
        }
    };

const buildAuditSession = (session: SessionItem, headers: APIGatewayProxyEvent["headers"]): SessionItem => ({
    ...session,
    clientIpAddress: headers[CLIENT_IP_HEADER],
});

const buildErrorResponse = (error: unknown): APIGatewayProxyResult => {
    const errorResponse = formatErrorResponse(error);
    if (error instanceof Error && !(error instanceof CriError)) {
        return {
            ...errorResponse,
            statusCode: 401,
            body: JSON.stringify(error.message),
        };
    }

    if (!(error instanceof CriError)) {
        return errorResponse;
    }

    if (error.statusCode === 400) {
        return {
            ...errorResponse,
            body: JSON.stringify(error.message),
        };
    }
    if (error.statusCode === 403) {
        return {
            ...errorResponse,
            body: JSON.stringify({
                error: "access_denied",
                error_description: "Access denied by resource owner or " + `authorization server - ${error.message}`,
            }),
        };
    }

    if (error.statusCode === 404) {
        return {
            ...errorResponse,
            body: JSON.stringify(error.message),
        };
    }

    if (error.statusCode === 408) {
        return {
            ...errorResponse,
            body: JSON.stringify(error.message),
        };
    }

    return errorResponse;
};

const getPostcodeFromRequest = (event: APIGatewayProxyEvent): string => {
    if (event.body === null || event.body === undefined) {
        throw new CriError(400, "Missing postcode in request body.");
    }

    let request: PostcodeRequest;

    try {
        request = JSON.parse(event.body) as PostcodeRequest;
    } catch {
        throw new CriError(400, "Failed to parse postcode from request body");
    }

    if (request?.postcode === null || request?.postcode === undefined) {
        throw new CriError(400, "Missing postcode in request body.");
    }

    return request.postcode;
};

const validatePostcode = (postcode: string): void => {
    if (!postcode.trim()) {
        throw new CriError(400, "Postcode must not be null or blank");
    }
};

const buildAuditRestricted = (postcode: string, headers: APIGatewayProxyEvent["headers"]): Record<string, unknown> => {
    const encodedDeviceInformation = headers[TXMA_AUDIT_ENCODED_HEADER];

    return {
        address: [
            {
                postalCode: decodePostcode(postcode).toUpperCase(),
            },
        ],
        ...(encodedDeviceInformation !== undefined && {
            device_information: {
                encoded: encodedDeviceInformation,
            },
        }),
    };
};
