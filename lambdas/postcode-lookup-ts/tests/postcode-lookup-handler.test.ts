import type { APIGatewayProxyEvent, Context } from "aws-lambda";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { buildAndSendAuditEvent } from "@govuk-one-login/cri-audit";
import { captureMetric, captureMetricWithDimensions } from "@govuk-one-login/cri-metrics";

import { type AuditConfig, PostcodeLookupHandler } from "../src/postcode-lookup-handler";
import { ApiError } from "../src/lib/error-handler";
import type { PostcodeLookupService } from "../src/services/postcode-lookup-service";
import type { SessionService } from "../src/services/session-service";

vi.mock("@govuk-one-login/cri-audit", () => ({
    buildAndSendAuditEvent: vi.fn(),
}));

vi.mock("@govuk-one-login/cri-metrics", () => ({
    captureMetric: vi.fn(),
    captureMetricWithDimensions: vi.fn(),
}));

const mockBuildAndSendAuditEvent = vi.mocked(buildAndSendAuditEvent);

const mockCaptureMetric = vi.mocked(captureMetric);

const mockCaptureMetricWithDimensions = vi.mocked(captureMetricWithDimensions);

describe("PostcodeLookupHandler", () => {
    const lookupPostcode = vi.fn();
    const validateSessionId = vi.fn();

    const auditConfig: AuditConfig = {
        queueUrl: "https://example.test/audit-queue",
        componentId: "https://example.test/address",
    };

    const postcodeLookupService = {
        lookupPostcode,
    } as unknown as PostcodeLookupService;

    const sessionService = {
        validateSessionId,
    } as unknown as SessionService;

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

    const createHandler = (): PostcodeLookupHandler =>
        new PostcodeLookupHandler(postcodeLookupService, sessionService, auditConfig);

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
        }) as APIGatewayProxyEvent;

    beforeEach(() => {
        lookupPostcode.mockReset();
        validateSessionId.mockReset();

        mockBuildAndSendAuditEvent.mockReset();
        mockCaptureMetric.mockReset();
        mockCaptureMetricWithDimensions.mockReset();

        mockBuildAndSendAuditEvent.mockResolvedValue(undefined);
    });

    it("returns 400 when request body is missing", async () => {
        const result = await createHandler().handler(createEvent(null), context);

        expect(result).toEqual({
            statusCode: 400,
            body: JSON.stringify({
                message: "Missing postcode in request body",
            }),
        });

        expect(validateSessionId).not.toHaveBeenCalled();
        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("returns 400 when request body contains invalid JSON", async () => {
        const result = await createHandler().handler(createEvent("{"), context);

        expect(result).toEqual({
            statusCode: 400,
            body: JSON.stringify({
                message: "Failed to parse postcode from request body",
            }),
        });

        expect(validateSessionId).not.toHaveBeenCalled();
        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("returns 400 when postcode value is missing", async () => {
        const result = await createHandler().handler(createEvent("{}"), context);

        expect(result).toEqual({
            statusCode: 400,
            body: JSON.stringify({
                message: "Missing postcode in request body",
            }),
        });

        expect(validateSessionId).not.toHaveBeenCalled();
        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("returns 400 when postcode value is blank", async () => {
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
            body: JSON.stringify({
                message: "Missing postcode in request body",
            }),
        });

        expect(validateSessionId).not.toHaveBeenCalled();
        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("returns 400 when session_id header is missing", async () => {
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
            statusCode: 400,
            body: JSON.stringify({
                message: "Missing header: session_id is required",
            }),
        });

        expect(validateSessionId).not.toHaveBeenCalled();
        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("calls postcode lookup using the client id from the session", async () => {
        validateSessionId.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue([]);

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(validateSessionId).toHaveBeenCalledWith("test-session-id");

        expect(lookupPostcode).toHaveBeenCalledWith("SW1A 2AA", "test-client");

        expect(result.statusCode).toBe(200);
    });

    it("returns addresses from postcode lookup service", async () => {
        const addresses = [
            {
                uprn: "123",
                postalCode: "SW1A 2AA",
            },
        ];

        validateSessionId.mockResolvedValue(session);
        lookupPostcode.mockResolvedValue(addresses);

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result.statusCode).toBe(200);
        expect(result.body).toBe(JSON.stringify(addresses));
    });

    it("sends request and response audit events", async () => {
        validateSessionId.mockResolvedValue(session);
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
            addresses: [
                {
                    postalCode: "SW1A 2AA",
                },
            ],
        };

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            1,
            auditConfig.queueUrl,
            "REQUEST_SENT",
            auditConfig.componentId,
            session,
            { restricted },
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            2,
            auditConfig.queueUrl,
            "RESPONSE_RECEIVED",
            auditConfig.componentId,
            session,
            { restricted },
        );
    });

    it("adds encoded device information to audit events", async () => {
        validateSessionId.mockResolvedValue(session);
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
            addresses: [
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
            "REQUEST_SENT",
            auditConfig.componentId,
            session,
            { restricted },
        );

        expect(mockBuildAndSendAuditEvent).toHaveBeenNthCalledWith(
            2,
            auditConfig.queueUrl,
            "RESPONSE_RECEIVED",
            auditConfig.componentId,
            session,
            { restricted },
        );
    });

    it("records the postcode lookup metric on success", async () => {
        validateSessionId.mockResolvedValue(session);
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
        validateSessionId.mockRejectedValue(new ApiError("Session not found", 403));

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
            body: JSON.stringify({
                message: "Session not found",
            }),
        });

        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("returns 403 when session has expired", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Session expired", 403));

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
            body: JSON.stringify({
                message: "Session expired",
            }),
        });

        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("returns 400 when client id is not supported", async () => {
        validateSessionId.mockResolvedValue({
            ...session,
            clientId: "unsupported-client",
        });

        lookupPostcode.mockRejectedValue(new ApiError("The Client ID provided for this session is not supported", 400));

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
            body: JSON.stringify({
                message: "The Client ID provided for this session is not supported",
            }),
        });

        expect(mockBuildAndSendAuditEvent).toHaveBeenCalledOnce();

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("returns 401 when session validation fails", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Any other exception", 401));

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
            body: JSON.stringify({
                message: "Any other exception",
            }),
        });

        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();
    });

    it("records lookup server dimensions for an unexpected error", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Any other exception", 401));

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
        validateSessionId.mockResolvedValue(session);
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
        validateSessionId.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new ApiError("Error Connection Timeout", 408));

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
            body: JSON.stringify({
                message: "Error Connection Timeout",
            }),
        });

        expect(mockBuildAndSendAuditEvent).toHaveBeenCalledOnce();

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("returns 404 when postcode lookup processing fails", async () => {
        validateSessionId.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new ApiError("Error sending request for postcode lookup", 404));

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
            body: JSON.stringify({
                message: "Error sending request for postcode lookup",
            }),
        });

        expect(mockBuildAndSendAuditEvent).toHaveBeenCalledOnce();

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("returns 500 for an unexpected error", async () => {
        validateSessionId.mockRejectedValue(new Error("Unexpected failure"));

        const result = await createHandler().handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result).toEqual({
            statusCode: 500,
            body: JSON.stringify({
                message: "Unexpected failure",
            }),
        });

        expect(lookupPostcode).not.toHaveBeenCalled();
        expect(mockBuildAndSendAuditEvent).not.toHaveBeenCalled();

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "lookup_server",
            postcode_lookup_error_message: "Unexpected_failure",
        });
    });

    it("records invalid postcode error dimensions", async () => {
        await createHandler().handler(createEvent("{"), context);

        expect(mockCaptureMetricWithDimensions).toHaveBeenCalledWith("postcode_lookup_error", {
            postcode_lookup_error_type: "invalid_postcode_param",
            postcode_lookup_error_message: "Failed_to_parse_postcode_from_request_body",
        });
    });

    it("records session-not-found error dimensions", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Session not found", 403));

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
        validateSessionId.mockRejectedValue(new ApiError("Session expired", 403));

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

    it("records timeout error dimensions", async () => {
        validateSessionId.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new ApiError("Error Connection Timeout", 408));

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
            postcode_lookup_error_message: "Error_Connection_Timeout",
        });
    });

    it("records lookup processing error dimensions", async () => {
        validateSessionId.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new ApiError("Error sending request for postcode lookup", 404));

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
        validateSessionId.mockResolvedValue({
            ...session,
            clientId: "unsupported-client",
        });

        lookupPostcode.mockRejectedValue(new ApiError("The Client ID provided for this session is not supported", 400));

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
        validateSessionId.mockResolvedValue(session);

        lookupPostcode.mockRejectedValue(new ApiError("Error sending request for postcode lookup", 404));

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
            "REQUEST_SENT",
            auditConfig.componentId,
            session,
            {
                restricted: {
                    addresses: [
                        {
                            postalCode: "SW1A 2AA",
                        },
                    ],
                },
            },
        );

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });
});
