import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

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

import {
    getAuthorizationCodeExpirationEpoch,
    getParameterValue,
    getSecretValue,
    getSessionExpirationEpoch,
} from "../src/services/configuration-service.js";

describe("configuration-service", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        delete process.env.SESSION_TTL;
        delete process.env.AUTHORIZATION_CODE_TTL;
    });

    describe("getParameterValue", () => {
        const stackName = "test-stack";
        const parameterName = "TestParameter";

        it("retrieves an SSM parameter using the stack-prefixed name", async () => {
            mockGetParameter.mockResolvedValue("test-value");

            await expect(getParameterValue(stackName, parameterName)).resolves.toBe("test-value");

            expect(mockGetParameter).toHaveBeenCalledOnce();

            expect(mockGetParameter).toHaveBeenCalledWith("/test-stack/TestParameter");
        });

        it("throws when the SSM parameter is missing", async () => {
            mockGetParameter.mockResolvedValue(undefined);

            await expect(getParameterValue(stackName, parameterName)).rejects.toThrow(
                "Missing parameter /test-stack/TestParameter",
            );

            expect(mockGetParameter).toHaveBeenCalledWith("/test-stack/TestParameter");

            expect(mockGetSecret).not.toHaveBeenCalled();
        });

        it("propagates errors returned by SSM", async () => {
            const error = new Error("SSM parameter retrieval failed");

            mockGetParameter.mockRejectedValue(error);

            await expect(getParameterValue(stackName, parameterName)).rejects.toBe(error);
        });
    });

    describe("getSecretValue", () => {
        const prefix = "test-prefix";
        const valueName = "TestValue";

        it("retrieves a value using the configured prefix", async () => {
            mockGetSecret.mockResolvedValue("test-value");

            await expect(getSecretValue(prefix, valueName)).resolves.toBe("test-value");

            expect(mockGetSecret).toHaveBeenCalledOnce();

            expect(mockGetSecret).toHaveBeenCalledWith("/test-prefix/TestValue");
        });

        it("decodes a binary value", async () => {
            mockGetSecret.mockResolvedValue(new TextEncoder().encode("test-value"));

            await expect(getSecretValue(prefix, valueName)).resolves.toBe("test-value");
        });

        it("throws when the value is missing", async () => {
            mockGetSecret.mockResolvedValue(undefined);

            await expect(getSecretValue(prefix, valueName)).rejects.toThrow("Missing secret /test-prefix/TestValue");

            expect(mockGetSecret).toHaveBeenCalledWith("/test-prefix/TestValue");

            expect(mockGetParameter).not.toHaveBeenCalled();
        });

        it("propagates errors returned by Secrets Manager", async () => {
            const error = new Error("Value retrieval failed");

            mockGetSecret.mockRejectedValue(error);

            await expect(getSecretValue(prefix, valueName)).rejects.toBe(error);
        });
    });
});
describe("expiration epoch ", () => {
    beforeEach(() => {
        vi.spyOn(Date, "now").mockReturnValue(1_000_123);
    });
    afterEach(() => {
        vi.restoreAllMocks();
    });
    describe("getSessionExpirationEpoch", () => {
        it("uses the default 2 hour session TTL", () => {
            expect(getSessionExpirationEpoch()).toBe(8200);
        });

        it("uses the configured session TTL", () => {
            process.env.SESSION_TTL = "3600";

            expect(getSessionExpirationEpoch()).toBe(4600);
        });
    });

    describe("getAuthorizationCodeExpirationEpoch", () => {
        it("uses the default 10 minute authorization code TTL", () => {
            expect(getAuthorizationCodeExpirationEpoch()).toBe(1600);
        });

        it("uses the configured authorization code TTL", () => {
            process.env.AUTHORIZATION_CODE_TTL = "300";

            expect(getAuthorizationCodeExpirationEpoch()).toBe(1300);
        });
    });
});
