import type { Config } from "jest";

export default {
    preset: "ts-jest",
    clearMocks: true,
    modulePaths: ["<rootDir>/src"],
    collectCoverageFrom: ["<rootDir>/src/**/*", "!<rootDir>/setEnvVars.js"],
    testMatch: ["<rootDir>/tests/**/*.test.ts"],
    setupFiles: ["<rootDir>/setEnvVars.js"],
    coverageThreshold: {
        global: {
            statements: 100,
            branches: 80,
            functions: 100,
            lines: 100,
        },
    },
    displayName: "lambdas/get-addresses",
} satisfies Config;
