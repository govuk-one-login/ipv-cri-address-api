import type { APIGatewayProxyEvent, Context } from "aws-lambda";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { logger } from "@govuk-one-login/cri-logger";
import { getParameterValue, getSecretValue } from "../../common/src/services/configuration-service.js";

vi.mock("../../common/src/services/configuration-service.js", () => ({
    getParameterValue: vi.fn(),
    getSecretValue: vi.fn(),
}));

const VALID_OS_API_URL = "https://test.os.uk/postcode";
const VALID_API_KEY = "a".repeat(32);

const TEN_DOWNING_STREET_RESPONSE = {
    results: [
        {
            DPA: {
                UPRN: "100023336956",
                BUILDING_NUMBER: "10",
                THOROUGHFARE_NAME: "DOWNING STREET",
                POST_TOWN: "LONDON",
                POSTCODE: "SW1A 2AA",
            },
        },
    ],
};

const mockGetParameterValue = vi.mocked(getParameterValue);

const mockGetSecretValue = vi.mocked(getSecretValue);

const loggerError = vi.spyOn(logger, "error");
const createEvent = (path: string): APIGatewayProxyEvent =>
    ({
        path,
    }) as APIGatewayProxyEvent;

const context = {} as Context;

const createHostResponse = (status = 200): Response =>
    ({
        status,
    }) as Response;

const createPostcodeResponse = (body: unknown = TEN_DOWNING_STREET_RESPONSE, status = 200): Response =>
    ({
        status,
        ok: status >= 200 && status < 300,
        text: vi.fn().mockResolvedValue(JSON.stringify(body)),
    }) as unknown as Response;

describe("HealthcheckHandler", () => {
    beforeEach(() => {
        vi.clearAllMocks();

        process.env.AWS_STACK_NAME = "test-stack";
        process.env.SECRET_PREFIX = "test-secret-prefix";
        process.env.CLIENT_ID = "ipv-core-stub-aws-prod";
        process.env.TIMEOUT_TIME_SECONDS = "20";
        process.env.EXPECTED_OS_API_KEY_LENGTH = "32";

        mockGetParameterValue.mockResolvedValue(VALID_OS_API_URL);

        mockGetSecretValue.mockResolvedValue(VALID_API_KEY);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    const getHandler = async () => {
        const { handler } = await import("../src/handler.js");

        return handler;
    };

    describe("/healthcheck/thirdparty", () => {
        it("returns 200 when all healthcheck assertions pass", async () => {
            const mockFetch = vi
                .fn()
                .mockResolvedValueOnce(createHostResponse(200))
                .mockResolvedValueOnce(createPostcodeResponse());

            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty"), context);

            expect(response.statusCode).toBe(200);
            expect(JSON.parse(response.body)).toEqual({
                message: "success",
            });

            expect(mockGetParameterValue).toHaveBeenCalledWith(
                "test-stack",
                "OrdnanceSurveyAPIUrl/ipv-core-stub-aws-prod",
            );

            expect(mockGetSecretValue).toHaveBeenCalledWith("test-secret-prefix", "OrdnanceSurveyAPIKey");

            expect(mockFetch).toHaveBeenCalledTimes(2);
        });

        it("fails when the OS API key has an unexpected length", async () => {
            mockGetSecretValue.mockResolvedValue("a".repeat(31));

            const mockFetch = vi.fn();
            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty"), context);

            expect(response.statusCode).not.toBe(200);

            expect(mockFetch).not.toHaveBeenCalled();
        });

        it("fails when the OS API host is unavailable", async () => {
            const mockFetch = vi.fn().mockResolvedValueOnce(createHostResponse(500));

            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty"), context);

            expect(response.statusCode).not.toBe(200);

            expect(mockFetch).toHaveBeenCalledOnce();

            expect(loggerError).toHaveBeenCalledWith("Healthcheck failed", {
                error: "Ordnance Survey API host is unavailable",
            });
        });

        it("fails when 10 Downing Street is not returned", async () => {
            const mockFetch = vi
                .fn()
                .mockResolvedValueOnce(createHostResponse(200))
                .mockResolvedValueOnce(
                    createPostcodeResponse({
                        results: [
                            {
                                DPA: {
                                    BUILDING_NUMBER: "11",
                                    THOROUGHFARE_NAME: "DOWNING STREET",
                                    POSTCODE: "SW1A 2AA",
                                },
                            },
                        ],
                    }),
                );

            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty"), context);

            expect(response.statusCode).not.toBe(200);
        });
    });

    describe("/healthcheck/thirdparty/info", () => {
        it("returns a successful healthcheck report", async () => {
            const mockFetch = vi
                .fn()
                .mockResolvedValueOnce(createHostResponse(200))
                .mockResolvedValueOnce(createPostcodeResponse());

            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

            expect(response.statusCode).toBe(200);

            const body = JSON.parse(response.body);

            expect(body.healthcheckPassed).toBe(true);

            expect(body.configuration.result).toEqual({
                apiUrl: VALID_OS_API_URL,
                apiKeyLength: 32,
                expectedApiKeyLength: 32,
            });

            expect(body.configuration.latency).toEqual(expect.any(Number));

            expect(body.host.result).toEqual({
                status: 200,
            });

            expect(body.host.latency).toEqual(expect.any(Number));

            expect(body.postcodeLookup.result).toEqual([
                expect.objectContaining({
                    buildingNumber: "10",
                    streetName: "DOWNING STREET",
                    postalCode: "SW1A 2AA",
                    addressCountry: "GB",
                }),
            ]);

            expect(body.postcodeLookup.latency).toEqual(expect.any(Number));
        });

        it("returns 200 and does not run later checks when configuration validation fails", async () => {
            mockGetSecretValue.mockResolvedValue("a".repeat(23));

            const mockFetch = vi.fn();
            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

            expect(response.statusCode).toBe(200);

            const body = JSON.parse(response.body);

            expect(body.healthcheckPassed).toBe(false);

            expect(body.configuration.error).toContain("Ordnance Survey API key has unexpected length");

            expect(body.host).toBe("N/A (not called due to earlier failures)");

            expect(body.postcodeLookup).toBe("N/A (not called due to earlier failures)");

            expect(mockFetch).not.toHaveBeenCalled();

            expect(loggerError).toHaveBeenCalledWith("Ordnance Survey configuration healthcheck failed", {
                error: expect.stringContaining("Ordnance Survey API key has unexpected length"),
            });
        });

        it("returns 200 and does not call OS Places after the host check fails", async () => {
            const mockFetch = vi.fn().mockResolvedValueOnce(createHostResponse(500));

            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

            expect(response.statusCode).toBe(200);

            const body = JSON.parse(response.body);

            expect(body.healthcheckPassed).toBe(false);

            expect(body.host.error).toContain("Ordnance Survey API host is unavailable");

            expect(body.postcodeLookup).toBe("N/A (not called due to earlier failures)");

            expect(mockFetch).toHaveBeenCalledOnce();

            expect(loggerError).toHaveBeenCalledWith("Ordnance Survey host healthcheck failed", {
                error: expect.stringContaining("Ordnance Survey API host is unavailable"),
            });
        });

        it("logs when the postcode healthcheck fails", async () => {
            const mockFetch = vi
                .fn()
                .mockResolvedValueOnce(createHostResponse(200))
                .mockResolvedValueOnce(
                    createPostcodeResponse({
                        results: [
                            {
                                DPA: {
                                    BUILDING_NUMBER: "11",
                                    THOROUGHFARE_NAME: "DOWNING STREET",
                                    POSTCODE: "SW1A 2AA",
                                },
                            },
                        ],
                    }),
                );

            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

            expect(response.statusCode).toBe(200);

            expect(loggerError).toHaveBeenCalledWith("Ordnance Survey postcode lookup healthcheck failed", {
                error: expect.stringContaining("10 Downing Street was not returned by the Ordnance Survey API"),
            });
        });

        it("does not expose the OS API key in the report", async () => {
            const mockFetch = vi
                .fn()
                .mockResolvedValueOnce(createHostResponse(200))
                .mockResolvedValueOnce(createPostcodeResponse());

            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

            expect(response.statusCode).toBe(200);

            expect(response.body).not.toContain(VALID_API_KEY);

            const body = JSON.parse(response.body);

            expect(body.configuration.result.apiKeyLength).toBe(32);
        });

        it("does not expose third-party error content in the report", async () => {
            const sensitiveError = "OS returned sensitive third-party details";

            const mockFetch = vi
                .fn()
                .mockResolvedValueOnce(createHostResponse(200))
                .mockResolvedValueOnce({
                    status: 500,
                    ok: false,
                    text: vi.fn().mockResolvedValue(sensitiveError),
                });

            vi.stubGlobal("fetch", mockFetch);

            const handler = await getHandler();

            const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

            expect(response.statusCode).toBe(200);

            expect(response.body).not.toContain(sensitiveError);

            const body = JSON.parse(response.body);

            expect(body.postcodeLookup.error).toContain("Ordnance Survey API returned status 500");
        });
    });

    it("returns an error for an unexpected path", async () => {
        const handler = await getHandler();

        const response = await handler(createEvent("/unexpected"), context);

        expect(response.statusCode).toBe(400);
    });

    it("returns 200 and skips later checks when the OS API URL cannot be retrieved", async () => {
        mockGetParameterValue.mockRejectedValue(new Error("SSM unavailable"));

        const mockFetch = vi.fn();
        vi.stubGlobal("fetch", mockFetch);

        const handler = await getHandler();

        const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

        expect(response.statusCode).toBe(200);

        const body = JSON.parse(response.body);

        expect(body.healthcheckPassed).toBe(false);
        expect(body.configuration.error).toContain("SSM unavailable");
        expect(body.host).toBe("N/A (not called due to earlier failures)");
        expect(body.postcodeLookup).toBe("N/A (not called due to earlier failures)");
        expect(mockFetch).not.toHaveBeenCalled();
    });

    it("returns 200 and skips later checks when the OS API key cannot be retrieved", async () => {
        mockGetSecretValue.mockRejectedValue(new Error("Secrets Manager unavailable"));

        const mockFetch = vi.fn();
        vi.stubGlobal("fetch", mockFetch);

        const handler = await getHandler();

        const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

        expect(response.statusCode).toBe(200);

        const body = JSON.parse(response.body);

        expect(body.healthcheckPassed).toBe(false);
        expect(body.configuration.error).toContain("Secrets Manager unavailable");
        expect(body.host).toBe("N/A (not called due to earlier failures)");
        expect(body.postcodeLookup).toBe("N/A (not called due to earlier failures)");
        expect(mockFetch).not.toHaveBeenCalled();
    });

    it("reports an invalid OS API URL and skips later checks", async () => {
        mockGetParameterValue.mockResolvedValue("not-a-valid-url");

        const mockFetch = vi.fn();
        vi.stubGlobal("fetch", mockFetch);

        const handler = await getHandler();

        const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

        expect(response.statusCode).toBe(200);

        const body = JSON.parse(response.body);

        expect(body.healthcheckPassed).toBe(false);
        expect(body.configuration.error).toContain("Ordnance Survey API URL is invalid");
        expect(mockFetch).not.toHaveBeenCalled();
    });

    it("returns a sanitised report when the OS host request rejects", async () => {
        const sensitiveError = "secret-internal-host";

        const mockFetch = vi.fn().mockRejectedValueOnce(new Error(sensitiveError));

        vi.stubGlobal("fetch", mockFetch);

        const handler = await getHandler();

        const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

        expect(response.statusCode).toBe(200);
        expect(response.body).not.toContain(sensitiveError);

        const body = JSON.parse(response.body);

        expect(body.healthcheckPassed).toBe(false);
        expect(body.host.error).toContain("Unable to contact the Ordnance Survey API host");
        expect(body.postcodeLookup).toBe("N/A (not called due to earlier failures)");
    });

    it("returns a sanitised report when the postcode request rejects", async () => {
        const sensitiveError = "request failed with sensitive headers";

        const mockFetch = vi
            .fn()
            .mockResolvedValueOnce(createHostResponse(200))
            .mockRejectedValueOnce(new Error(sensitiveError));

        vi.stubGlobal("fetch", mockFetch);

        const handler = await getHandler();

        const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

        expect(response.statusCode).toBe(200);
        expect(response.body).not.toContain(sensitiveError);

        const body = JSON.parse(response.body);

        expect(body.healthcheckPassed).toBe(false);
        expect(body.postcodeLookup.error).toContain("Unable to call the Ordnance Survey API");
    });

    it("does not expose a malformed OS response in the report", async () => {
        const malformedResponse = "invalid response containing SW1A 2AA";

        const mockFetch = vi
            .fn()
            .mockResolvedValueOnce(createHostResponse(200))
            .mockResolvedValueOnce({
                status: 200,
                ok: true,
                text: vi.fn().mockResolvedValue(malformedResponse),
            });

        vi.stubGlobal("fetch", mockFetch);

        const handler = await getHandler();

        const response = await handler(createEvent("/healthcheck/thirdparty/info"), context);

        expect(response.statusCode).toBe(200);
        expect(response.body).not.toContain(malformedResponse);

        const body = JSON.parse(response.body);

        expect(body.healthcheckPassed).toBe(false);
        expect(body.postcodeLookup.error).toContain("Unable to process the Ordnance Survey API response");
    });

    it("accepts 10 Downing Street amongst multiple results without exact response matching", async () => {
        const mockFetch = vi
            .fn()
            .mockResolvedValueOnce(createHostResponse(401))
            .mockResolvedValueOnce(
                createPostcodeResponse({
                    results: [
                        {
                            DPA: {
                                BUILDING_NUMBER: "11",
                                THOROUGHFARE_NAME: "DOWNING STREET",
                                POSTCODE: "SW1A 2AA",
                            },
                        },
                        {
                            DPA: {
                                BUILDING_NUMBER: " 10 ",
                                THOROUGHFARE_NAME: "downing street",
                                POSTCODE: "sw1a2aa",
                            },
                        },
                    ],
                }),
            );

        vi.stubGlobal("fetch", mockFetch);

        const handler = await getHandler();

        const response = await handler(createEvent("/healthcheck/thirdparty"), context);

        expect(response.statusCode).toBe(200);
    });
});
