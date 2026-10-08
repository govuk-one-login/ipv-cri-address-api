import { Logger } from "@aws-lambda-powertools/logger";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { ConfigurationService } from "../../src/services/configuration-service";
import { PostcodeLookupService } from "../../src/services/postcode-lookup-service";

const TEST_URL = "https://test.os.uk/postcode";
const TEST_API_KEY = "mock-api-key";

describe("PostcodeLookupService", () => {
    const getParameterValue = vi.fn();
    const getSecretValue = vi.fn();

    const mockConfigurationService = {
        getParameterValue,
        getSecretValue,
    } as unknown as ConfigurationService;

    const mockLogger = {
        info: vi.fn(),
    } as unknown as Logger;

    beforeEach(() => {
        vi.clearAllMocks();

        getParameterValue.mockResolvedValue(TEST_URL);
        getSecretValue.mockResolvedValue(TEST_API_KEY);
    });

    const createService = (fetchFn: typeof fetch): PostcodeLookupService =>
        new PostcodeLookupService(mockLogger, mockConfigurationService, fetchFn);

    it("returns empty array for 404 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toEqual([]);
    });

    it("returns empty array for 400 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 400,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    error: {
                        statuscode: 400,
                        message: "Bad request",
                    },
                }),
            ),
        });

        const service = createService(mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toEqual([]);
    });

    it("returns mapped addresses for 200 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 200,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    results: [
                        {
                            DPA: {
                                UPRN: "123",
                                POSTCODE: "SW1A 2AA",
                                POST_TOWN: "London",
                            },
                        },
                    ],
                }),
            ),
        });

        const service = createService(mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toHaveLength(1);
        expect(result[0]).toMatchObject({
            uprn: "123",
            postalCode: "SW1A 2AA",
            addressLocality: "London",
            addressCountry: "GB",
        });
    });

    it("throws for 500 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 500,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    error: {
                        statuscode: 500,
                        message: "Unexpected error",
                    },
                }),
            ),
        });

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toThrow(
            "Error processing postcode lookup",
        );
    });

    it("throws when postcode is empty", async () => {
        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("", "test-client")).rejects.toThrow("Postcode must not be null or blank");

        expect(getParameterValue).not.toHaveBeenCalled();
        expect(getSecretValue).not.toHaveBeenCalled();
        expect(mockFetch).not.toHaveBeenCalled();
    });

    it("throws when postcode is whitespace", async () => {
        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("   ", "test-client")).rejects.toThrow(
            "Postcode must not be null or blank",
        );

        expect(getParameterValue).not.toHaveBeenCalled();
        expect(getSecretValue).not.toHaveBeenCalled();
        expect(mockFetch).not.toHaveBeenCalled();
    });

    it("builds a postcode lookup URL", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(mockFetch).toHaveBeenCalledWith(expect.stringContaining("postcode=SW1A+2AA"), expect.any(Object));
    });

    it("sends required headers", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(mockFetch).toHaveBeenCalledWith(
            expect.any(String),
            expect.objectContaining({
                headers: {
                    Accept: "application/json",
                    key: TEST_API_KEY,
                },
            }),
        );
    });

    it("throws when fetch fails", async () => {
        const mockFetch = vi.fn().mockRejectedValue(new Error("network failure"));

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toThrow(
            "Error sending request for postcode lookup",
        );
    });

    it("uses the configured API key when making requests", async () => {
        const configuredHeaderValue = "test-header-value";

        getSecretValue.mockResolvedValue(configuredHeaderValue);

        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(mockFetch).toHaveBeenCalledWith(
            expect.any(String),
            expect.objectContaining({
                headers: expect.objectContaining({
                    key: configuredHeaderValue,
                }),
            }),
        );
    });

    it("uses the configured API URL when building requests", async () => {
        const configuredUrl = "https://configured.os.uk/postcode";

        getParameterValue.mockResolvedValue(configuredUrl);

        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(mockFetch).toHaveBeenCalledWith(expect.stringContaining(configuredUrl), expect.any(Object));
    });

    it("retrieves the OS URL for the supplied client id", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(getParameterValue).toHaveBeenCalledWith("OrdnanceSurveyAPIUrl/test-client");
    });

    it("retrieves the OS API key", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(getSecretValue).toHaveBeenCalledWith("OrdnanceSurveyAPIKey");
    });

    it("throws 400 when client configuration cannot be retrieved", async () => {
        getParameterValue.mockRejectedValue(new Error("Parameter not found"));

        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "unsupported-client")).rejects.toMatchObject({
            message: "The Client ID provided for this session is not supported",
            statusCode: 400,
        });

        expect(getSecretValue).not.toHaveBeenCalled();
        expect(mockFetch).not.toHaveBeenCalled();
    });

    it("throws 400 when the OS API key cannot be retrieved", async () => {
        getSecretValue.mockRejectedValue(new Error("Secret not found"));

        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toMatchObject({
            message: "The Client ID provided for this session is not supported",
            statusCode: 400,
        });

        expect(getParameterValue).toHaveBeenCalledWith("OrdnanceSurveyAPIUrl/test-client");
        expect(mockFetch).not.toHaveBeenCalled();
    });

    it("creates an audit event context", () => {
        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        const session = {
            sessionId: "test-session-id",
            clientId: "test-client",
        };

        const headers = {
            session_id: "test-session-id",
        };

        const result = service.getAuditEventContext("LS1 1BA", headers, session as never);

        expect(result).toEqual({
            personIdentity: {
                addresses: [
                    {
                        postalCode: "LS1 1BA",
                    },
                ],
            },
            requestHeaders: headers,
            session,
        });
    });

    it("uppercases the postcode in the audit context", () => {
        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        const result = service.getAuditEventContext("ls1 1ba", {}, {} as never);

        expect(result.personIdentity.addresses[0].postalCode).toBe("LS1 1BA");
    });

    it("decodes an encoded postcode", () => {
        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        const result = service.getAuditEventContext("LS1%201BA", {}, {} as never);

        expect(result.personIdentity.addresses[0].postalCode).toBe("LS1 1BA");
    });

    it("throws when request headers are missing", () => {
        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        expect(() => service.getAuditEventContext("LS1 1BA", undefined as never, {} as never)).toThrow(
            "requestHeaders must not be null",
        );
    });

    it("throws when session item is missing", () => {
        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        expect(() => service.getAuditEventContext("LS1 1BA", {}, undefined as never)).toThrow(
            "sessionItem must not be null",
        );
    });
    it("creates an audit event context", () => {
        const service = createService(vi.fn() as typeof fetch);

        const session = {
            sessionId: "session-id",
            clientId: "test-client",
        };

        const headers = {
            session_id: "session-id",
        };

        const result = service.getAuditEventContext("LS1 1BA", headers, session);

        expect(result).toEqual({
            personIdentity: {
                addresses: [
                    {
                        postalCode: "LS1 1BA",
                    },
                ],
            },
            requestHeaders: headers,
            session,
        });
    });
    it("throws when request headers are undefined", () => {
        const service = createService(vi.fn() as typeof fetch);

        expect(() =>
            service.getAuditEventContext(
                "LS1 1BA",
                undefined as never,
                {
                    sessionId: "session-id",
                    clientId: "client-id",
                } as never,
            ),
        ).toThrow("requestHeaders must not be null");
    });

    it("throws when session item is undefined", () => {
        const service = createService(vi.fn() as typeof fetch);

        expect(() =>
            service.getAuditEventContext(
                "LS1 1BA",
                {
                    session_id: "test",
                },
                undefined as never,
            ),
        ).toThrow("sessionItem must not be null");
    });

    it("uppercases the postcode in audit event context", () => {
        const service = createService(vi.fn() as typeof fetch);

        const result = service.getAuditEventContext(
            "ls1 1ba",
            {
                session_id: "test",
            },
            {
                sessionId: "session-id",
                clientId: "client-id",
            } as never,
        );

        expect(result.personIdentity.addresses[0].postalCode).toBe("LS1 1BA");
    });

    it("decodes URL encoded postcodes", () => {
        const service = createService(vi.fn() as typeof fetch);

        const result = service.getAuditEventContext(
            "LS1%201BA",
            {
                session_id: "test",
            },
            {
                sessionId: "session-id",
                clientId: "client-id",
            } as never,
        );

        expect(result.personIdentity.addresses[0].postalCode).toBe("LS1 1BA");
    });
});
