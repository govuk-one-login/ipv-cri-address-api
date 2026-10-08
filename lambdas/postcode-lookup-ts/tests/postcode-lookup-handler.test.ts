import { APIGatewayProxyEvent, Context } from "aws-lambda";
import { describe, expect, it, vi, beforeEach } from "vitest";

import { PostcodeLookupHandler } from "../src/postcode-lookup-handler";
import { PostcodeLookupService } from "../src/services/postcode-lookup-service";
import { ApiError } from "../src/lib/error-handler";
import { AuditEventType } from "../src/services/audit-service";

const validateSessionId = vi.fn();

const mockSessionService = {
    validateSessionId,
};
const sendAuditEvent = vi.fn();
const mockAuditService = {
    sendAuditEvent,
};
const getAuditEventContext = vi.fn();
const counterMetric = vi.fn();
const addDimensions = vi.fn();

const mockEventProbe = {
    counterMetric,
    addDimensions,
};

describe("PostcodeLookupHandler", () => {
    const lookupPostcode = vi.fn();

    const mockService = {
        lookupPostcode,
        getAuditEventContext,
    } as unknown as PostcodeLookupService;

    const context = {
        functionName: "postcode-lookup",
    } as Context;

    beforeEach(() => {
        vi.clearAllMocks();
    });

    const createEvent = (body: string | null): APIGatewayProxyEvent =>
        ({
            body,
            headers: {
                session_id: "test-session-id",
            },
        }) as APIGatewayProxyEvent;

    it("returns 400 when request body is missing", async () => {
        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(createEvent(null), context);

        expect(result?.statusCode).toBe(400);

        expect(result?.body).toBe(
            JSON.stringify({
                message: "Missing postcode in request body",
            }),
        );

        expect(lookupPostcode).not.toHaveBeenCalled();
    });

    it("returns 400 when request body is invalid json", async () => {
        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(createEvent("{"), context);

        expect(result?.statusCode).toBe(400);

        expect(result?.body).toBe(
            JSON.stringify({
                message: "Failed to parse postcode from request body",
            }),
        );

        expect(lookupPostcode).not.toHaveBeenCalled();
    });

    it("returns 400 when postcode value is missing", async () => {
        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(createEvent("{}"), context);

        expect(result?.statusCode).toBe(400);

        expect(lookupPostcode).not.toHaveBeenCalled();
    });

    it("returns 400 when postcode value is blank", async () => {
        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "   ",
                }),
            ),
            context,
        );

        expect(result?.statusCode).toBe(400);

        expect(lookupPostcode).not.toHaveBeenCalled();
    });

    it("calls postcode lookup service", async () => {
        lookupPostcode.mockResolvedValue([
            {
                uprn: "123",
                postalCode: "SW1A 2AA",
            },
        ]);
        validateSessionId.mockResolvedValue({
            clientId: "test-client",
        });
        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(lookupPostcode).toHaveBeenCalledWith("SW1A 2AA", "test-client");

        expect(result?.statusCode).toBe(200);
    });
    it("returns addresses from postcode lookup service", async () => {
        const addresses = [
            {
                uprn: "123",
                postalCode: "SW1A 2AA",
            },
        ];

        lookupPostcode.mockResolvedValue(addresses);
        validateSessionId.mockResolvedValue({
            clientId: "test-client",
        });
        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result?.body).toBe(JSON.stringify(addresses));
    });

    it("returns 400 when session_id header is missing", async () => {
        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(
            {
                body: JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
                headers: {},
            } as APIGatewayProxyEvent,
            context,
        );

        expect(result?.statusCode).toBe(400);

        expect(lookupPostcode).not.toHaveBeenCalled();
    });

    it("uses client id from session when looking up postcode", async () => {
        lookupPostcode.mockResolvedValue([]);

        validateSessionId.mockResolvedValue({
            clientId: "client-from-session",
        });

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(validateSessionId).toHaveBeenCalledWith("test-session-id");

        expect(lookupPostcode).toHaveBeenCalledWith("SW1A 2AA", "client-from-session");
    });

    it("returns 403 when session is not found", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Session not found", 403));
        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);
        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result?.statusCode).toBe(403);
        expect(lookupPostcode).not.toHaveBeenCalled();
    });

    it("returns 403 when session has expired", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Session expired", 403));
        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);
        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result?.statusCode).toBe(403);
        expect(lookupPostcode).not.toHaveBeenCalled();
    });

    it("returns 400 when client id is not supported", async () => {
        validateSessionId.mockResolvedValue({
            clientId: "unsupported-client",
        });

        lookupPostcode.mockRejectedValue(new ApiError("The Client ID provided for this session is not supported", 400));

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result?.statusCode).toBe(400);
    });

    it("returns 401 when session validation fails", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Any other exception", 401));

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result?.statusCode).toBe(401);
        expect(lookupPostcode).not.toHaveBeenCalled();
    });

    it("returns 408 when postcode lookup times out", async () => {
        validateSessionId.mockResolvedValue({
            clientId: "test-client",
        });

        lookupPostcode.mockRejectedValue(new ApiError("Error Connection Timeout", 408));

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result?.statusCode).toBe(408);
    });

    it("returns 404 when postcode lookup processing fails", async () => {
        validateSessionId.mockResolvedValue({
            clientId: "test-client",
        });

        lookupPostcode.mockRejectedValue(new ApiError("Error sending request for postcode lookup", 404));

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result?.statusCode).toBe(404);
    });

    it("sends request and response audit events", async () => {
        validateSessionId.mockResolvedValue({
            clientId: "test-client",
        });

        lookupPostcode.mockResolvedValue([]);

        getAuditEventContext.mockReturnValue({
            test: true,
        });

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(sendAuditEvent).toHaveBeenNthCalledWith(1, AuditEventType.REQUEST_SENT, { test: true });

        expect(sendAuditEvent).toHaveBeenNthCalledWith(2, AuditEventType.RESPONSE_RECEIVED, { test: true });
    });

    it("records postcode lookup metric", async () => {
        validateSessionId.mockResolvedValue({
            clientId: "test-client",
        });

        lookupPostcode.mockResolvedValue([]);

        getAuditEventContext.mockReturnValue({
            test: true,
        });

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(counterMetric).toHaveBeenCalledWith("postcode_lookup");
    });

    it("records a postcode error metric", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Session not found", 403));

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(counterMetric).toHaveBeenCalledWith("postcode_lookup_error");
    });

    it("records session-not-found error dimensions", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Session not found", 403));

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(counterMetric).toHaveBeenCalledWith("postcode_lookup_error");

        expect(addDimensions).toHaveBeenCalledWith({
            postcode_lookup_error_type: "session_not_found",
            postcode_lookup_error_message: "Session_not_found",
        });
    });

    it("records session-expired error dimensions", async () => {
        validateSessionId.mockRejectedValue(new ApiError("Session expired", 403));

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(addDimensions).toHaveBeenCalledWith({
            postcode_lookup_error_type: "session_expired",
            postcode_lookup_error_message: "Session_expired",
        });
    });

    it("records timeout error dimensions", async () => {
        validateSessionId.mockResolvedValue({
            clientId: "test-client",
        });

        getAuditEventContext.mockReturnValue({
            test: true,
        });

        lookupPostcode.mockRejectedValue(new ApiError("Error Connection Timeout", 408));

        const handler = new PostcodeLookupHandler(mockService, mockSessionService, mockAuditService, mockEventProbe);

        await handler.handler(
            createEvent(
                JSON.stringify({
                    postcode: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(addDimensions).toHaveBeenCalledWith({
            postcode_lookup_error_type: "time_out_error",
            postcode_lookup_error_message: "Error_Connection_Timeout",
        });
    });
});
