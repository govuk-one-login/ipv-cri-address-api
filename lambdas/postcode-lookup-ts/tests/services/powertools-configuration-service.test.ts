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
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("retrieves parameter values", async () => {
        mockGetParameter.mockResolvedValue("value");

        const service = new PowertoolsConfigurationService();

        await expect(service.getParameterValue("test-parameter")).resolves.toBe("value");

        expect(mockGetParameter).toHaveBeenCalledWith("test-parameter");
    });

    it("retrieves secret values", async () => {
        mockGetSecret.mockResolvedValue("secret-value");

        const service = new PowertoolsConfigurationService();

        await expect(service.getSecretValue("test-secret")).resolves.toBe("secret-value");

        expect(mockGetSecret).toHaveBeenCalledWith("test-secret");
    });

    it("throws when parameter is missing", async () => {
        mockGetParameter.mockResolvedValue(undefined);

        const service = new PowertoolsConfigurationService();

        await expect(service.getParameterValue("missing")).rejects.toThrow("Missing parameter missing");
    });

    it("throws when secret is missing", async () => {
        mockGetSecret.mockResolvedValue(undefined);

        const service = new PowertoolsConfigurationService();

        await expect(service.getSecretValue("missing-secret")).rejects.toThrow("Missing secret missing-secret");
    });
});
