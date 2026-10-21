import type { APIGatewayProxyEvent, Context } from "aws-lambda";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { buildAndSendAuditEvent } from "@govuk-one-login/cri-audit";
import { CriError } from "@govuk-one-login/cri-error-response";
import { captureMetric, captureMetricWithDimensions } from "@govuk-one-login/cri-metrics";
import { logger } from "@govuk-one-login/cri-logger";

import { createHandler as createPostcodeLookupHandler } from "../src/postcode-lookup-handler.js";

vi.mock("@govuk-one-login/cri-logger", () => ({
    logger: {
        info: vi.fn(),
        error: vi.fn(),
    },
}));

vi.mock("@govuk-one-login/cri-audit", () => ({
    buildAndSendAuditEvent: vi.fn(),
}));

vi.mock("@govuk-one-login/cri-metrics", () => ({
    captureMetric: vi.fn(),
    captureMetricWithDimensions: vi.fn(),
}));

const mockLoggerError = vi.mocked(logger.error);

const mockBuildAndSendAuditEvent = vi.mocked(buildAndSendAuditEvent);

const mockCaptureMetric = vi.mocked(captureMetric);

const mockCaptureMetricWithDimensions = vi.mocked(captureMetricWithDimensions);

describe("createHandler", () => {
    const lookupPostcode = vi.fn();
    const getSessionById = vi.fn();

    const auditConfig = {
        queueUrl: "https://example.test/audit-queue",
        componentId: "https://example.test/address",
        auditEventPrefix: "IPV_ADDRESS_CRI",
    };

    const context = {
        functionName: "postcode-lookup",
    } as Context;

    const session = {
        sessionId: "test-session-id",
        clientId: "test-client",
        clientSessionId: "test-client-session-id",
        subject: "test-subject",
        persistentSessionId: "test-persistent-session-id",
    };

    const createHandler = () => ({
        handler: createPostcodeLookupHandler({
            ...auditConfig,
            lookupPostcode,
            getSessionById,
        }),
    });

    const createEvent = (
        body: string | null,
        additionalHeaders: Record<string, string | undefined> = {},
    ): APIGatewayProxyEvent =>
        ({
            body,
            headers: {
                session_id: "test-session-id",
                ...additionalHeaders,
            },
        }) as unknown as APIGatewayProxyEvent;

    beforeEach(() => {
        lookupPostcode.mockReset();
        getSessionById.mockReset();

        mockBuildAndSendAuditEvent.mockReset();
        mockCaptureMetric.mockReset();
        mockCaptureMetricWithDimensions.mockReset();
        mockLoggerError.mockReset();

        mockBuildAndSendAuditEvent.mockResolvedValue(undefined);
    });
    it.each([
        ["null", null, "Missing postcode in request body."],
        ["empty", "", "Failed to parse postcode from request body"],
        ["invalid JSON", "{", "Failed to parse postcode from request body"],
        ["missing postcode value", "{}", "Missing postcode in request body."],
        ["JSON null", "null", "Missing postcode in request body."],
    ])("returns 400 when request body is %s", async (_scenario, body, expectedMessage) => {
        const result = await createHandler().handler(createEvent(body), context);

        expect(result).toEqual({
            statusCode: 400,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(expectedMessage),
        });

        expect(getSessionById).not.toHaveBeenCalled();
        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("returns 400 when the postcode is blank", async () => {
        getSessionById.mockResolvedValue(session);

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "   ",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 400,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify("Postcode must not be null or blank"),
        });

        expect(getSessionById).toHaveBeenCalledWith("test-session-id");
        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("validates the session before rejecting a blank postcode", async () => {
        getSessionById.mockRejectedValue(new CriError(403, "Session not found"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "   ",
                }),
            ),
            context,
        );

        expect(getSessionById).toHaveBeenCalled();
        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();

        expect(result).toEqual({
            statusCode: 403,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                error: "access_denied",
                error_description: "Access denied by resource owner or authorization server - Session not found",
            }),
        });
    });

    it("returns 403 when session_id header is missing", async () => {
        const result = await createHandler().handler(
            {
                body: JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
                headers: {},
            } as APIGatewayProxyEvent,
            context,
        );

        expect(result).toEqual({
            statusCode: 403,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                error: "access_denied",
                error_description: "Access denied by resource owner or authorization server - Session not found",
            }),
        });

        expect(getSessionById).not.toHaveBeenCalled();
        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("calls postcode lookup using the client id from the session", async () => {
        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue([]);

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(getSessionById).toHaveBeenCalledWith("test-session-id");

        expect(lookupPostcode).toHaveBeenCalledWith("SW1A 2AA", "test-client");

        expect(result).toEqual({
            statusCode: 200,
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify([]),
        });
    });

    it("returns addresses from postcode lookup service", async () => {
        const address = [
            {
                uprn: "123",
                postalCode: "SW1A 2AA",
            },
        ];

        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue(address);

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 200,
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(address),
        });
    });

    it("sends request and response audit events", async () => {
        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue([]);

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        const restricted = {
            address: [
                {
                    postalCode: "SW1A 2AA",
                },
            ],
        };

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            1,
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_REQUEST_SENT",
            auditConfig.componentId,
            session,
            { restricted },
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            2,
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_RESPONSE_RECEIVED",
            auditConfig.componentId,
            session,
            { restricted },
        );
    });

    it("uses the configured audit event prefix", async () => {
        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue([]);

        const handler = createPostcodeLookupHandler({
            ...auditConfig,
            auditEventPrefix: "TEST_PREFIX",
            lookupPostcode,
            getSessionById,
        });

        await handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            1,
            auditConfig.queueUrl,
            "TEST_PREFIX_REQUEST_SENT",
            auditConfig.componentId,
            session,
            expect.anything(),
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            2,
            auditConfig.queueUrl,
            "TEST_PREFIX_RESPONSE_RECEIVED",
            auditConfig.componentId,
            session,
            expect.anything(),
        );
    });

    it("adds encoded device information to audit events", async () => {
        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue([]);

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A%202AA",
                }),
                {
                    "txma-audit-encoded": "encoded-device-information",
                },
            ),
            context,
        );

        const restricted = {
            address: [
                {
                    postalCode: "SW1A 2AA",
                },
            ],
            device_information: {
                encoded: "encoded-device-information",
            },
        };

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            1,
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_REQUEST_SENT",
            auditConfig.componentId,
            session,
            { restricted },
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            2,
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_RESPONSE_RECEIVED",
            auditConfig.componentId,
            session,
            { restricted },
        );
    });

    it("uses X-Forwarded-For as the IP address for request and response audit events", async () => {
        getSessionById.mockResolvedValue({
            ...session,
            clientIpAddress: "10.0.0.1",
        });
        lookupPostcode.mockResolvedValue([]);

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
                {
                    "X-Forwarded-For": "203.0.113.10",
                },
            ),
            context,
        );

        const expectedAuditSession = {
            ...session,
            clientIpAddress: "203.0.113.10",
        };

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            1,
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_REQUEST_SENT",
            auditConfig.componentId,
            expectedAuditSession,
            {
                restricted: {
                    address: [
                        {
                            postalCode: "SW1A 2AA",
                        },
                    ],
                },
            },
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            2,
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_RESPONSE_RECEIVED",
            auditConfig.componentId,
            expectedAuditSession,
            {
                restricted: {
                    address: [
                        {
                            postalCode: "SW1A 2AA",
                        },
                    ],
                },
            },
        );
    });

    it("records the postcode lookup metric on success", async () => {
        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue([]);

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(mockCaptureMetric).toHaveBeenCalledOnce();
        expect(mockCaptureMetric).toHaveBeenCalledWith("postcode_lookup");

        expect(mockCaptureMetricWithDimensions).not.toHaveBeenCalled();
    });

    it("returns 403 when session is not found", async () => {
        getSessionById.mockRejectedValue(new CriError(403, "Session not found"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 403,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                error: "access_denied",
                error_description: "Access denied by resource owner or authorization server - Session not found",
            }),
        });

        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("returns 403 when session has expired", async () => {
        getSessionById.mockRejectedValue(new CriError(403, "Session expired"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 403,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                error: "access_denied",
                error_description: "Access denied by resource owner or authorization server - Session expired",
            }),
        });

        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("returns 400 when client id is not supported", async () => {
        getSessionById.mockResolvedValue({
            ...session,
            clientId: "unsupported-client",
        });

        lookupPostcode.mockRejectedValue(new CriError(400, "The Client ID provided for this session is not supported"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 400,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify("The Client ID provided for this session is not supported"),
        });

        expect(mockBuildAndSendAuditEvent).toHaveBeenCalledOnce();

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("returns 401 when session validation fails", async () => {
        getSessionById.mockRejectedValue(new CriError(401, "Any other exception"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 401,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                message: "Any other exception",
            }),
        });

        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("does not log the postcode when a non-Error value is thrown", async () => {
        const postcode = "SW1A 2AA";

        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockRejectedValue({
            message: "Unexpected postcode lookup error",
            postcode,
        });

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode,
                }),
            ),
            context,
        );

        expect(mockLoggerError).toHaveBeenCalledWith(
            "Postcode lookup failed",
            expect.objectContaining({
                message: "Unknown postcode lookup error",
            }),
        );

        expect(JSON.stringify(mockLoggerError.mock.calls)).not.toContain(postcode);
    });

    it("does not log the postcode when an Error contains the postcode", async () => {
        const postcode = "SW1A 2AA";

        getSessionById.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new Error(`Unexpected error for ${postcode}`));

        await createHandler().handler(createEvent(JSON.stringify({ postcode })), context);

        expect(mockLoggerError).toHaveBeenCalled();

        const loggedError = mockLoggerError.mock.calls[0][1];

        expect(loggedError).toBeInstanceOf(Error);

        if (loggedError instanceof Error) {
            expect(loggedError.message).not.toContain(postcode);
        }
    });

    it("does not record the postcode in error metric dimensions", async () => {
        const postcode = "SW1A 2AA";

        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockRejectedValue(new Error(`Unexpected error for ${postcode}`));

        await createHandler().handler(createEvent(JSON.stringify({ postcode })), context);

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalled();

        const dimensions = mockCaptureMetricWithDimensions.mock.calls[0][1];

        expect(JSON.stringify(dimensions)).not.toContain(postcode);
    });

    it("records lookup server dimensions for an unexpected error", async () => {
        getSessionById.mockRejectedValue(new CriError(401, "Any other exception"));

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "lookup_server",
            postcode_lookup_error_message: "Any_other_exception",
        });
    });

    it("performs audit, lookup and metric operations in Java-equivalent order", async () => {
        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue([]);

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        const requestAuditOrder = mockBuildAndSendAuditEvent.mock.invocationCallOrder[0];

        const lookupOrder = lookupPostcode.mock.invocationCallOrder[0];

        const metricOrder = mockCaptureMetric.mock.invocationCallOrder[0];

        const responseAuditOrder = mockBuildAndSendAuditEvent.mock.invocationCallOrder[1];

        expect(requestAuditOrder).toBeLessThan(lookupOrder);
        expect(lookupOrder).toBeLessThan(metricOrder);
        expect(metricOrder).toBeLessThan(responseAuditOrder);
    });

    it("returns 408 when postcode lookup times out", async () => {
        getSessionById.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new CriError(408, "Error timed out waiting for postcode lookup response"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 408,
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify("Error timed out waiting for postcode lookup response"),
        });

        expect(mockBuildAndSendAuditEvent).toHaveBeenCalledOnce();

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("returns 404 when postcode lookup processing fails", async () => {
        getSessionById.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new CriError(404, "Error sending request for postcode lookup"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 404,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify("Error sending request for postcode lookup"),
        });

        expect(mockBuildAndSendAuditEvent).toHaveBeenCalledOnce();

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("returns 401 for an unexpected error", async () => {
        getSessionById.mockRejectedValue(new Error("Unexpected failure"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 401,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify("Unexpected failure"),
        });

        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "lookup_server",
            postcode_lookup_error_message: "Unexpected_failure",
        });
    });

    it("returns 401 when parsing an empty OS success response fails", async () => {
        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockRejectedValue(new SyntaxError("Unexpected end of JSON input"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "M25 4RT",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 401,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify("Unexpected end of JSON input"),
        });
        expect(mockBuildAndSendAuditEvent).toHaveBeenCalledOnce();
        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("records invalid postcode error dimensions", async () => {
        await createHandler().handler(createEvent("{"), context);

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "invalid_postcode_param",
            postcode_lookup_error_message: "Failed_to_parse_postcode_from_request_body",
        });
    });

    it("records session-not-found error dimensions", async () => {
        getSessionById.mockRejectedValue(new CriError(403, "Session not found"));

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "session_not_found",
            postcode_lookup_error_message: "Session_not_found",
        });
    });

    it("records session-expired error dimensions", async () => {
        getSessionById.mockRejectedValue(new CriError(403, "Session expired"));

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "session_expired",
            postcode_lookup_error_message: "Session_expired",
        });
    });

    it("includes empty device information when txma-audit-encoded is present but empty", async () => {
        getSessionById.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue([]);

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
                {
                    "txma-audit-encoded": "",
                },
            ),
            context,
        );

        const restricted = {
            address: [
                {
                    postalCode: "SW1A 2AA",
                },
            ],
            device_information: {
                encoded: "",
            },
        };

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            1,
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_REQUEST_SENT",
            auditConfig.componentId,
            session,
            { restricted },
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            2,
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_RESPONSE_RECEIVED",
            auditConfig.componentId,
            session,
            { restricted },
        );
    });

    it("records timeout error dimensions", async () => {
        getSessionById.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new CriError(408, "Error_timed_out_waiting_for_postcode_lookup_response"));

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "time_out_error",
            postcode_lookup_error_message: "Error_timed_out_waiting_for_postcode_lookup_response",
        });
    });

    it("records lookup processing error dimensions", async () => {
        getSessionById.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new CriError(404, "Error sending request for postcode lookup"));

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "lookup_processing",
            postcode_lookup_error_message: "Error_sending_request_for_postcode_lookup",
        });
    });

    it("records lookup server error dimensions", async () => {
        getSessionById.mockResolvedValue({
            ...session,
            clientId: "unsupported-client",
        });

        lookupPostcode.mockRejectedValue(new CriError(400, "The Client ID provided for this session is not supported"));

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "lookup_server",
            postcode_lookup_error_message: "The_Client_ID_provided_for_this_session_is_not_supported",
        });
    });

    it("does not send the response audit event when lookup fails", async () => {
        getSessionById.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new CriError(404, "Error sending request for postcode lookup"));

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenCalledOnce();

        expect(mockBuildAndSendAuditEvent).toHaveBeenCalledWith(
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_REQUEST_SENT",
            auditConfig.componentId,
            session,
            {
                restricted: {
                    address: [
                        {
                            postalCode: "SW1A 2AA",
                        },
                    ],
                },
            },
        );

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("uses X-Forwarded-For as the audit event IP address for the request audit event", async () => {
        getSessionById.mockResolvedValue({
            ...session,
            clientIpAddress: "10.0.0.1",
        });
        lookupPostcode.mockResolvedValue([]);

        await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
                {
                    "X-Forwarded-For": "203.0.113.10",
                },
            ),
            context,
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            1,
            auditConfig.queueUrl,
            "IPV_ADDRESS_CRI_REQUEST_SENT",
            auditConfig.componentId,
            {
                ...session,
                clientIpAddress: "203.0.113.10",
            },
            expect.anything(),
        );
    });
});
