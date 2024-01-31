import type { Config } from "jest";

export default {
    preset: "ts-jest/presets/default-esm",
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
    displayName: "lambdas/get-addresses",
    transform: {
        // '^.+\\.m?[tj]sx?$' to process js/ts/mjs/mts with `ts-jest`
        "^.+\\.[tj]sx?$": [
            "ts-jest",
            {
                useESM: true,
            },
        ],
    },
} satisfies Config;
