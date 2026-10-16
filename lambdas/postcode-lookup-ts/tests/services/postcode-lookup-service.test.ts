import { logger } from "@govuk-one-login/cri-logger";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { captureMetric, MetricUnit } from "@govuk-one-login/cri-metrics";

vi.mock("../../src/services/configuration-service", () => ({
    getParameterValue: vi.fn(),
    getSecretValue: vi.fn(),
}));

vi.mock("@govuk-one-login/cri-metrics", async (importOriginal) => {
    const actual = await importOriginal<typeof import("@govuk-one-login/cri-metrics")>();

    return {
        ...actual,
        captureMetric: vi.fn(),
    };
});

import { lookupPostcode as performPostcodeLookup } from "../../src/services/postcode-lookup-service.js";

const TEST_URL = "https://test.os.uk/postcode";
const TEST_API_KEY = "mock-api-key";

describe("lookupPostcode", () => {
    const getParameterValue = vi.fn();
    const getSecretValue = vi.fn();
    const mockCaptureMetric = vi.mocked(captureMetric);

    const loggerError = vi.spyOn(logger, "error");

    beforeEach(() => {
        vi.clearAllMocks();

        getParameterValue.mockResolvedValue(TEST_URL);
        getSecretValue.mockResolvedValue(TEST_API_KEY);
    });

    const createService = (fetchFn: typeof fetch) => ({
        lookupPostcode: (postcode: string, clientId: string) =>
            performPostcodeLookup(postcode, clientId, {
                stackName: "test-stack",
                secretPrefix: "test-secret-prefix",
                getParameterValue: async (_stackName, parameterName) => getParameterValue(parameterName),
                getSecretValue: async (_secretPrefix, secretName) => getSecretValue(secretName),
                fetchFn,
            }),
    });

    it("returns empty array for 404 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toEqual([]);
    });

    it("returns empty array and logs sanitized details for 400 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 400,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    error: {
                        statuscode: 400,
                        message:
                            "Requested postcode must contain a minimum of the sector plus 1 digit of the district e.g. SO1. Requested postcode was 5WF12LZ",
                    },
                }),
            ),
        });

        const service = createService(mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toEqual([]);
        expect(loggerError).toHaveBeenCalledWith(
            "Ordnance Survey Responded with status 400: Requested postcode must contain a minimum of the sector plus 1 digit of the district e.g. SO1. Requested postcode was *******",
        );
    });

    it.each([
        { body: "invalid response for SW1A 1AA", sanitizedBody: "invalid response for ********" },
        { body: "", sanitizedBody: "" },
    ])("returns empty array and logs unknown error for 400 body %j", async ({ body, sanitizedBody }) => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 400,
            text: vi.fn().mockResolvedValue(body),
        });

        const service = createService(mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toEqual([]);
        expect(loggerError).toHaveBeenCalledWith(`Ordnance Survey Responded with unknown error: ${sanitizedBody}`);
    });

    it("returns mapped addresses for 200 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 200,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    results: [
                        {
                            DPA: {
                                UPRN: 123,
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
            uprn: 123,
            postalCode: "SW1A 2AA",
            addressLocality: "London",
            addressCountry: "GB",
        });
    });

    it("records postcode lookup duration in milliseconds and logs the same latency", async () => {
        const loggerInfoSpy = vi.spyOn(logger, "info").mockImplementation(() => undefined);

        const mockFetch = vi.fn().mockResolvedValue(
            new Response(
                JSON.stringify({
                    results: [],
                }),
                {
                    status: 200,
                },
            ),
        );

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(mockCaptureMetric).toHaveBeenCalledOnce();

        const durationMs = mockCaptureMetric.mock.calls[0][1];

        expect(mockCaptureMetric).toHaveBeenCalledWith("lookup_postcode_duration", durationMs, MetricUnit.Milliseconds);

        expect(loggerInfoSpy).toHaveBeenCalledWith("API response received from OS API", {
            status: 200,
            latencyInMs: durationMs,
        });
    });

    it("does not record postcode lookup duration when the OS request fails", async () => {
        const mockFetch = vi.fn().mockRejectedValue(new Error("Network failure"));

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toMatchObject({
            statusCode: 404,
            message: "Error sending request for postcode lookup",
        });

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("does not record postcode lookup duration when reading the OS response fails", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 200,
            text: vi.fn().mockRejectedValue(new Error("Failed to read response body")),
        });

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toMatchObject({
            statusCode: 404,
            message: "Error sending request for postcode lookup",
        });

        expect(mockCaptureMetric).not.toHaveBeenCalled();
    });

    it("throws a 404 processing error for OS rate-limit responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 429,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    error: {
                        statuscode: 429,
                        message: "Rate limit exceeded",
                    },
                }),
            ),
        });

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toMatchObject({
            message: "Error processing postcode lookup: Rate limit exceeded",
            statusCode: 404,
        });
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

    it("decodes form-encoded spaces in the postcode before building the lookup URL", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A+2AA", "test-client");

        const [requestUrl] = mockFetch.mock.calls[0] as [string, RequestInit];

        expect(new URL(requestUrl).searchParams.get("postcode")).toBe("SW1A 2AA");
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

    it("throws 404 when fetch fails", async () => {
        const mockFetch = vi.fn().mockRejectedValue(new Error("network failure"));

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toMatchObject({
            statusCode: 404,
            message: "Error sending request for postcode lookup",
        });
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
    it("configures a 15 second timeout on postcode lookup requests", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A 2AA", "test-client");

        const [, requestOptions] = mockFetch.mock.calls[0] as [string, RequestInit];

        expect(requestOptions.signal).toBeDefined();
    });

    it("throws 408 when the postcode lookup times out", async () => {
        const timeoutError = new DOMException("The operation timed out", "TimeoutError");

        const mockFetch = vi.fn().mockRejectedValue(timeoutError);

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toMatchObject({
            statusCode: 408,
            message: "Error timed out waiting for postcode lookup response",
        });
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

    it("throws 401 when the OS API key cannot be retrieved", async () => {
        getSecretValue.mockRejectedValue(new Error("Secret not found"));

        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toMatchObject({
            message: "Unable to retrieve the Ordnance Survey API key",
            statusCode: 401,
        });

        expect(getParameterValue).toHaveBeenCalledWith("OrdnanceSurveyAPIUrl/test-client");

        expect(mockFetch).not.toHaveBeenCalled();
    });

    it("throws when a 200 response has an empty body", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 200,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toThrow(SyntaxError);
    });

    it("returns empty array for a 200 response with no results", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 200,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    results: [],
                }),
            ),
        });

        const service = createService(mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toEqual([]);
    });

    it("returns empty array for a 200 response containing a null DPA", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 200,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    results: [
                        {
                            DPA: null,
                        },
                    ],
                }),
            ),
        });

        const service = createService(mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toEqual([]);
    });

    it("does not include the API key in the request URL", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = createService(mockFetch as typeof fetch);

        await service.lookupPostcode("SW1A 2AA", "test-client");

        const [requestUrl] = mockFetch.mock.calls[0] as [string, RequestInit];

        const url = new URL(requestUrl);

        expect(url.searchParams.has("key")).toBe(false);
        expect(requestUrl).not.toContain(TEST_API_KEY);
    });

    it("throws when the configured OS API URL is invalid", async () => {
        getParameterValue.mockResolvedValue("invalidURL{}");

        const mockFetch = vi.fn();

        const service = createService(mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toMatchObject({
            message: "Error building URI for postcode lookup",
            statusCode: 400,
        });

        expect(mockFetch).not.toHaveBeenCalled();
    });
});
