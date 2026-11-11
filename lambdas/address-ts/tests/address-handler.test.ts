import type { APIGatewayProxyEvent, Context } from "aws-lambda";

import { CriError } from "@govuk-one-login/cri-error-response";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { createHandler } from "../src/address-handler.js";

const { mockCaptureMetric } = vi.hoisted(() => ({
    mockCaptureMetric: vi.fn(),
}));

vi.mock("@govuk-one-login/cri-logger", () => ({
    logger: {
        info: vi.fn(),
        error: vi.fn(),
    },
}));

vi.mock("@govuk-one-login/cri-metrics", () => ({
    captureMetric: mockCaptureMetric,
}));

const SESSION_ID = "00000000-0000-0000-0000-000000000001";
const ADDRESS_TTL = 1730197212;

const context = {} as Context;

const event = {
    headers: {
        session_id: SESSION_ID,
    },
    body: "some json",
} as unknown as APIGatewayProxyEvent;

const canonicalAddress = {
    uprn: 12345,
    validFrom: "2013-08-09",
    addressCountry: "GB",
};

describe("address-handler", () => {
    const parseAddresses = vi.fn();
    const getSessionById = vi.fn();
    const applyAddressValidity = vi.fn();
    const getSessionExpirationEpoch = vi.fn();
    const saveAddresses = vi.fn();
    const storeAddressEntryTypeMetrics = vi.fn();
    const createAuthorizationCode = vi.fn();

    const handler = createHandler({
        parseAddresses,
        getSessionById,
        applyAddressValidity,
        getSessionExpirationEpoch,
        saveAddresses,
        storeAddressEntryTypeMetrics,
        createAuthorizationCode,
    });

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("returns 200 when addresses are empty", async () => {
        parseAddresses.mockReturnValueOnce([]);

        const response = await handler(event, context);

        expect(response).toEqual({
            statusCode: 200,
            body: "",
        });

        expect(getSessionById).not.toHaveBeenCalled();
        expect(applyAddressValidity).not.toHaveBeenCalled();
        expect(getSessionExpirationEpoch).not.toHaveBeenCalled();
        expect(saveAddresses).not.toHaveBeenCalled();
        expect(storeAddressEntryTypeMetrics).not.toHaveBeenCalled();
        expect(createAuthorizationCode).not.toHaveBeenCalled();
        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("returns 204 and saves addresses for a valid session", async () => {
        const session = {
            sessionId: SESSION_ID,
            expiryDate: ADDRESS_TTL,
        };

        const addressesWithValidity = [canonicalAddress];

        parseAddresses.mockReturnValueOnce([canonicalAddress]);

        getSessionById.mockResolvedValueOnce(session);

        applyAddressValidity.mockReturnValueOnce(addressesWithValidity);

        getSessionExpirationEpoch.mockReturnValueOnce(ADDRESS_TTL);

        const response = await handler(event, context);

        expect(response).toEqual({
            statusCode: 204,
            body: "",
        });

        expect(getSessionById).toHaveBeenCalledWith(SESSION_ID);

        expect(applyAddressValidity).toHaveBeenCalledWith([canonicalAddress]);

        expect(getSessionExpirationEpoch).toHaveBeenCalledOnce();

        expect(saveAddresses).toHaveBeenCalledWith(SESSION_ID, addressesWithValidity, ADDRESS_TTL);

        expect(storeAddressEntryTypeMetrics).toHaveBeenCalledWith(addressesWithValidity);

        expect(createAuthorizationCode).toHaveBeenCalledWith(session);

        expect(mockCaptureMetric).toHaveBeenCalledWith("address");
    });

    it("returns 403 when the session cannot be found", async () => {
        parseAddresses.mockReturnValueOnce([canonicalAddress]);

        getSessionById.mockRejectedValueOnce(new CriError(403, "Session not found"));

        const response = await handler(event, context);

        expect(response).toEqual({
            statusCode: 403,
            body: JSON.stringify({
                error: "access_denied",
                error_description: "Access denied by resource owner or " + "authorization server - Session not found",
            }),
        });

        expect(mockCaptureMetric).toHaveBeenCalledWith("address", 0);

        expect(saveAddresses).not.toHaveBeenCalled();
        expect(createAuthorizationCode).not.toHaveBeenCalled();
    });

    it("returns 403 when the session has expired", async () => {
        parseAddresses.mockReturnValueOnce([canonicalAddress]);

        getSessionById.mockRejectedValueOnce(new CriError(403, "Session expired"));

        const response = await handler(event, context);

        expect(response).toEqual({
            statusCode: 403,
            body: JSON.stringify({
                error: "access_denied",
                error_description: "Access denied by resource owner or " + "authorization server - Session expired",
            }),
        });

        expect(mockCaptureMetric).toHaveBeenCalledWith("address", 0);

        expect(saveAddresses).not.toHaveBeenCalled();
        expect(createAuthorizationCode).not.toHaveBeenCalled();
    });

    it("returns 500 when parsing addresses fails", async () => {
        parseAddresses.mockImplementationOnce(() => {
            throw new CriError(500, "Country code not present for address");
        });

        const response = await handler(event, context);

        expect(response.statusCode).toBe(500);

        expect(getSessionById).not.toHaveBeenCalled();

        expect(mockCaptureMetric).toHaveBeenCalledWith("address", 0);
    });

    it("returns 500 when an unexpected error occurs", async () => {
        parseAddresses.mockImplementationOnce(() => {
            throw new Error("Unexpected parsing failure");
        });

        const response = await handler(event, context);

        expect(response).toEqual({
            statusCode: 500,
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                message: "Internal server error",
            }),
        });

        expect(mockCaptureMetric).toHaveBeenCalledWith("address", 0);

        expect(getSessionById).not.toHaveBeenCalled();
        expect(saveAddresses).not.toHaveBeenCalled();
        expect(createAuthorizationCode).not.toHaveBeenCalled();
    });

    it("does not create an authorization code when saving addresses fails", async () => {
        const session = {
            sessionId: SESSION_ID,
            expiryDate: ADDRESS_TTL,
        };

        const addresses = [canonicalAddress];

        parseAddresses.mockReturnValueOnce(addresses);
        getSessionById.mockResolvedValueOnce(session);
        applyAddressValidity.mockReturnValueOnce(addresses);
        getSessionExpirationEpoch.mockReturnValueOnce(ADDRESS_TTL);

        saveAddresses.mockRejectedValueOnce(new Error("Unable to save addresses"));

        const response = await handler(event, context);

        expect(response).toEqual({
            statusCode: 500,
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                message: "Internal server error",
            }),
        });

        expect(storeAddressEntryTypeMetrics).not.toHaveBeenCalled();

        expect(createAuthorizationCode).not.toHaveBeenCalled();

        expect(mockCaptureMetric).toHaveBeenCalledWith("address", 0);
    });

    it("returns 500 when authorization code creation fails", async () => {
        const session = {
            sessionId: SESSION_ID,
            expiryDate: ADDRESS_TTL,
        };

        const addresses = [canonicalAddress];

        parseAddresses.mockReturnValueOnce(addresses);
        getSessionById.mockResolvedValueOnce(session);
        applyAddressValidity.mockReturnValueOnce(addresses);
        getSessionExpirationEpoch.mockReturnValueOnce(ADDRESS_TTL);

        createAuthorizationCode.mockRejectedValueOnce(new Error("Unable to create authorization code"));

        const response = await handler(event, context);

        expect(response.statusCode).toBe(500);

        expect(saveAddresses).toHaveBeenCalledOnce();
        expect(createAuthorizationCode).toHaveBeenCalledWith(session);

        expect(mockCaptureMetric).toHaveBeenCalledWith("address", 0);

        expect(mockCaptureMetric).not.toHaveBeenCalledWith("address");
    });
});
