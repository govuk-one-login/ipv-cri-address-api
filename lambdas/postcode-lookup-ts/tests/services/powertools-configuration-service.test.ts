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

import { PowertoolsConfigurationService } from "../../src/services/powertools-configuration-service";

describe("PowertoolsConfigurationService", () => {
    const stackName = "test-stack";
    const secretPrefix = "test-secret-prefix";

    const createService = (): PowertoolsConfigurationService =>
        new PowertoolsConfigurationService(stackName, secretPrefix);

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("retrieves an SSM parameter using the stack-prefixed name", async () => {
        mockGetParameter.mockResolvedValue("https://example.test/postcode");

        const service = createService();

        await expect(service.getParameterValue("OrdnanceSurveyAPIUrl/test-client")).resolves.toBe(
            "https://example.test/postcode",
        );

        expect(mockGetParameter).toHaveBeenCalledOnce();

        expect(mockGetParameter).toHaveBeenCalledWith("/test-stack/OrdnanceSurveyAPIUrl/test-client");
    });

    it("retrieves a secret using the configured secret prefix", async () => {
        mockGetSecret.mockResolvedValue("test-api-key");

        const service = createService();

        await expect(service.getSecretValue("OrdnanceSurveyAPIKey")).resolves.toBe("test-api-key");

        expect(mockGetSecret).toHaveBeenCalledOnce();

        expect(mockGetSecret).toHaveBeenCalledWith("/test-secret-prefix/OrdnanceSurveyAPIKey");
    });

    it("throws when the SSM parameter is missing", async () => {
        mockGetParameter.mockResolvedValue(undefined);

        const service = createService();

        await expect(service.getParameterValue("OrdnanceSurveyAPIUrl/test-client")).rejects.toThrow(
            "Missing parameter /test-stack/OrdnanceSurveyAPIUrl/test-client",
        );

        expect(mockGetParameter).toHaveBeenCalledWith("/test-stack/OrdnanceSurveyAPIUrl/test-client");

        expect(mockGetSecret).not.toHaveBeenCalled();
    });

    it("throws when the secret is missing", async () => {
        mockGetSecret.mockResolvedValue(undefined);

        const service = createService();

        await expect(service.getSecretValue("OrdnanceSurveyAPIKey")).rejects.toThrow(
            "Missing secret /test-secret-prefix/OrdnanceSurveyAPIKey",
        );

        expect(mockGetSecret).toHaveBeenCalledWith("/test-secret-prefix/OrdnanceSurveyAPIKey");

        expect(mockGetParameter).not.toHaveBeenCalled();
    });

    it("propagates errors returned by SSM", async () => {
        const error = new Error("SSM parameter retrieval failed");

        mockGetParameter.mockRejectedValue(error);

        const service = createService();

        await expect(service.getParameterValue("OrdnanceSurveyAPIUrl/test-client")).rejects.toBe(error);
    });

    it("propagates errors returned by Secrets Manager", async () => {
        const error = new Error("Secret retrieval failed");

        mockGetSecret.mockRejectedValue(error);

        const service = createService();

        await expect(service.getSecretValue("OrdnanceSurveyAPIKey")).rejects.toBe(error);
    });
});
