import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier/flat";
import { defineConfig, globalIgnores } from "eslint/config";
import tseslint from "typescript-eslint";
import globals from "globals";

export default defineConfig(
    js.configs.recommended,
    tseslint.configs.recommended,
    eslintConfigPrettier,
    globalIgnores(["**/.aws-sam/**"]),
    {
        languageOptions: {
            globals: globals.node,
        },
        linterOptions: {
            reportUnusedInlineConfigs: "error",
        },
        rules: {
            "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
        },
    },
);
