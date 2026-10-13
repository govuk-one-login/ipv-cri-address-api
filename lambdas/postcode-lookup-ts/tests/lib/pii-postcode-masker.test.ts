import { sanitize } from "../../src/lib/pii-postcode-masker";
import { describe, expect, it } from "vitest";

describe("PiiPostcodeMasker", () => {
    it("valid postcode can be masked", () => {
        expect(sanitize("SW1A 1AA")).toBe("********");
        expect(sanitize("EC1A 1BB")).toBe("********");
        expect(sanitize("W1A 0AX")).toBe("*******");
    });

    it("postcode in sentence can be masked", () => {
        expect(sanitize("My address is SW1A 1AA")).toBe("My address is ********");

        expect(sanitize("EC1A 1BB is in London")).toBe("******** is in London");
    });

    it("multiple postcodes in sentence can be masked", () => {
        expect(sanitize("W1A 0AX and SW1A 2AA are valid postcodes")).toBe("******* and ******** are valid postcodes");
    });

    it("empty string returns empty string", () => {
        expect(sanitize("")).toBe("");
    });

    it("null input returns null", () => {
        expect(sanitize(null)).toBeNull();
    });

    it("no postcode in sentence returns sentence unchanged", () => {
        expect(sanitize("This is just a normal sentence.")).toBe("This is just a normal sentence.");
    });

    it.each(["B16 9BL", "sw1a 1aa", "EC1A 1BB"])("lowercase postcode can be masked: %s", (postcode) => {
        expect(sanitize(postcode)).toBe("*".repeat(postcode.length));
    });

    it("postcode with extra spaces can be masked", () => {
        expect(sanitize("   SW1A 1AA   ")).toBe("   ********   ");
    });

    it.each(["B16 9BL", "sw1a 1aa", "EC1A 1BB", "ABC", "123"])("postcode-like value can be masked: %s", (postcode) => {
        expect(sanitize(`SO1. Requested postcode was ${postcode}`)).toBe(
            `SO1. Requested postcode was ${"*".repeat(postcode.length)}`,
        );
    });
});
