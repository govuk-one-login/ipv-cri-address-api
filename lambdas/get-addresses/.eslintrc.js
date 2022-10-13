module.exports = {
    parser: "@typescript-eslint/parser",
    parserOptions: {
        ecmaVersion: 2020, // Allows for the parsing of modern ECMAScript features
        sourceType: "module",
    },
    env: {
        node: true,
        es2020: true,
        jest: true,
    },
    globals: {
        sinon: true,
        expect: true,
    },
    root: true,
    extends: ["eslint:recommended", "prettier", "plugin:@typescript-eslint/recommended"],
    rules: {
        "no-console": 2,
        "padding-line-between-statements": ["error", { blankLine: "any", prev: "*", next: "*" }],
    },
};