import { describe, expect, it } from "vitest";

import { decodePostcode, getPostcodeFromRequest, validatePostcode } from "../../src/lib/postcode.js";
import { PostcodeLookupError } from "../../src/types/postcode-lookup-error.js";

describe("getPostcodeFromRequest", () => {
    it("returns the postcode from a valid request body", () => {
        const result = getPostcodeFromRequest(JSON.stringify({ value: "SW1A 2AA" }));

        expect(result).toBe("SW1A 2AA");
    });

    it("preserves whitespace around a non-blank postcode", () => {
        const result = getPostcodeFromRequest(JSON.stringify({ value: " SW1A 2AA " }));

        expect(result).toBe(" SW1A 2AA ");
    });

    it("throws BAD_REQUEST when the body is null", () => {
        expect(() => getPostcodeFromRequest(null)).toThrow(
            expect.objectContaining({
                name: "PostcodeLookupError",
                code: "BAD_REQUEST",
                message: "Missing postcode in request body.",
            }),
        );
    });

    it("throws BAD_REQUEST when the body is empty", () => {
        expect(() => getPostcodeFromRequest("")).toThrow(
            expect.objectContaining({
                code: "BAD_REQUEST",
                message: "Missing postcode in request body.",
            }),
        );
    });

    it("throws BAD_REQUEST when the body contains invalid JSON", () => {
        expect(() => getPostcodeFromRequest("{")).toThrow(
            expect.objectContaining({
                code: "BAD_REQUEST",
                message: "Failed to parse postcode from request body",
            }),
        );
    });

    it("throws BAD_REQUEST when value is missing", () => {
        expect(() => getPostcodeFromRequest("{}")).toThrow(
            expect.objectContaining({
                code: "BAD_REQUEST",
                message: "Missing postcode in request body.",
            }),
        );
    });

    it("throws BAD_REQUEST when value is null", () => {
        expect(() => getPostcodeFromRequest(JSON.stringify({ value: null }))).toThrow(
            expect.objectContaining({
                code: "BAD_REQUEST",
                message: "Missing postcode in request body.",
            }),
        );
    });

    it("throws BAD_REQUEST when value is not a string", () => {
        expect(() => getPostcodeFromRequest(JSON.stringify({ value: 123 }))).toThrow(
            expect.objectContaining({
                code: "BAD_REQUEST",
                message: "Missing postcode in request body.",
            }),
        );
    });

    it("throws VALIDATION when the postcode is empty", () => {
        expect(() => getPostcodeFromRequest(JSON.stringify({ value: "" }))).toThrow(
            expect.objectContaining({
                code: "VALIDATION",
                message: "Postcode must not be null or blank",
            }),
        );
    });

    it("throws VALIDATION when the postcode contains only whitespace", () => {
        expect(() => getPostcodeFromRequest(JSON.stringify({ value: "   " }))).toThrow(
            expect.objectContaining({
                code: "VALIDATION",
                message: "Postcode must not be null or blank",
            }),
        );
    });
});

describe("validatePostcode", () => {
    it("returns a valid postcode unchanged", () => {
        expect(validatePostcode("SW1A 2AA")).toBe("SW1A 2AA");
    });

    it("throws a PostcodeLookupError for a blank postcode", () => {
        expect(() => validatePostcode(" ")).toThrow(PostcodeLookupError);
    });
});

describe("decodePostcode", () => {
    it("decodes percent-encoded spaces", () => {
        expect(decodePostcode("SW1A%202AA")).toBe("SW1A 2AA");
    });

    it("decodes plus signs as spaces", () => {
        expect(decodePostcode("SW1A+2AA")).toBe("SW1A 2AA");
    });

    it("leaves a normal postcode unchanged", () => {
        expect(decodePostcode("SW1A 2AA")).toBe("SW1A 2AA");
    });
});
