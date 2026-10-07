import { APIGatewayProxyEvent, Context } from "aws-lambda";
import { describe, expect, it, vi, beforeEach } from "vitest";

import { PostcodeLookupHandler } from "../src/postcode-lookup-handler";
import { PostcodeLookupService } from "../src/services/postcode-lookup-service";

const getSession = vi.fn();

const mockSessionService = {
    getSession,
};

describe("PostcodeLookupHandler", () => {
    const lookupPostcode = vi.fn();

    const mockService = {
        lookupPostcode,
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
        const handler = new PostcodeLookupHandler(mockService, mockSessionService);

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
        const handler = new PostcodeLookupHandler(mockService, mockSessionService);

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
        const handler = new PostcodeLookupHandler(mockService, mockSessionService);

        const result = await handler.handler(createEvent("{}"), context);

        expect(result?.statusCode).toBe(400);

        expect(lookupPostcode).not.toHaveBeenCalled();
    });

    it("returns 400 when postcode value is blank", async () => {
        const handler = new PostcodeLookupHandler(mockService, mockSessionService);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    value: "   ",
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
        getSession.mockResolvedValue({
            clientId: "test-client",
        });
        const handler = new PostcodeLookupHandler(mockService, mockSessionService);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    value: "SW1A 2AA",
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
        getSession.mockResolvedValue({
            clientId: "test-client",
        });
        const handler = new PostcodeLookupHandler(mockService, mockSessionService);

        const result = await handler.handler(
            createEvent(
                JSON.stringify({
                    value: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(result?.body).toBe(JSON.stringify(addresses));
    });

    it("returns 400 when session_id header is missing", async () => {
        const handler = new PostcodeLookupHandler(mockService, mockSessionService);

        const result = await handler.handler(
            {
                body: JSON.stringify({
                    value: "SW1A 2AA",
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

        getSession.mockResolvedValue({
            clientId: "client-from-session",
        });

        const handler = new PostcodeLookupHandler(mockService, mockSessionService);

        await handler.handler(
            createEvent(
                JSON.stringify({
                    value: "SW1A 2AA",
                }),
            ),
            context,
        );

        expect(getSession).toHaveBeenCalledWith("test-session-id");

        expect(lookupPostcode).toHaveBeenCalledWith("SW1A 2AA", "client-from-session");
    });
});
