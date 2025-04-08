import type { Config } from "jest";

export default {
    preset: "ts-jest",
    clearMocks: true,
    modulePaths: ["<rootDir>/src"],
    collectCoverageFrom: ["<rootDir>/src/**/*"],
    testMatch: ["<rootDir>/tests/**/*.test.ts"],
    coverageThreshold: {
        global: {
            statements: 100,
            branches: 80,
            functions: 100,
            lines: 100,
        },
    },
    displayName: "lambdas/time-function",
} satisfies Config;
