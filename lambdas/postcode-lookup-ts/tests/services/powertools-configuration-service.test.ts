import { beforeEach, describe, expect, it, vi } from "vitest";

const { mockGetParameter, mockGetSecret } = vi.hoisted(() => ({
    mockGetParameter: vi.fn(),
    mockGetSecret: vi.fn(),
}));

vi.mock("@aws-lambda-powertools/parameters/ssm", () => ({
    getParameter: mockGetParameter,
}));

vi.mock("@aws-lambda-powertools/parameters/secrets", () => ({
    getSecret: mockGetSecret,
}));

import { getParameterValue, getSecretValue } from "../../src/services/configuration-service";

describe("configuration functions", () => {
    const stackName = "test-stack";
    const secretPrefix = "test-secret-prefix";

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("retrieves an SSM parameter using the stack-prefixed name", async () => {
        mockGetParameter.mockResolvedValue("https://example.test/postcode");

        await expect(getParameterValue(stackName, "OrdnanceSurveyAPIUrl/test-client")).resolves.toBe(
            "https://example.test/postcode",
        );

        expect(mockGetParameter).toHaveBeenCalledOnce();

        expect(mockGetParameter).toHaveBeenCalledWith("/test-stack/OrdnanceSurveyAPIUrl/test-client");
    });

    it("retrieves a secret using the configured secret prefix", async () => {
        mockGetSecret.mockResolvedValue("test-api-key");

        await expect(getSecretValue(secretPrefix, "OrdnanceSurveyAPIKey")).resolves.toBe("test-api-key");

        expect(mockGetSecret).toHaveBeenCalledOnce();

        expect(mockGetSecret).toHaveBeenCalledWith("/test-secret-prefix/OrdnanceSurveyAPIKey");
    });

    it("throws when the SSM parameter is missing", async () => {
        mockGetParameter.mockResolvedValue(undefined);

        await expect(getParameterValue(stackName, "OrdnanceSurveyAPIUrl/test-client")).rejects.toThrow(
            "Missing parameter /test-stack/OrdnanceSurveyAPIUrl/test-client",
        );

        expect(mockGetParameter).toHaveBeenCalledWith("/test-stack/OrdnanceSurveyAPIUrl/test-client");

        expect(mockGetSecret).not.toHaveBeenCalled();
    });

    it("throws when the secret is missing", async () => {
        mockGetSecret.mockResolvedValue(undefined);

        await expect(getSecretValue(secretPrefix, "OrdnanceSurveyAPIKey")).rejects.toThrow(
            "Missing secret /test-secret-prefix/OrdnanceSurveyAPIKey",
        );

        expect(mockGetSecret).toHaveBeenCalledWith("/test-secret-prefix/OrdnanceSurveyAPIKey");

        expect(mockGetParameter).not.toHaveBeenCalled();
    });

    it("propagates errors returned by SSM", async () => {
        const error = new Error("SSM parameter retrieval failed");

        mockGetParameter.mockRejectedValue(error);

        await expect(getParameterValue(stackName, "OrdnanceSurveyAPIUrl/test-client")).rejects.toBe(error);
    });

    it("propagates errors returned by Secrets Manager", async () => {
        const error = new Error("Secret retrieval failed");

        mockGetSecret.mockRejectedValue(error);

        await expect(getSecretValue(secretPrefix, "OrdnanceSurveyAPIKey")).rejects.toBe(error);
    });
});
