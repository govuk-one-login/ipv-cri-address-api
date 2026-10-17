import { describe, expect, it } from "vitest";

import { decodePostcode } from "../../src/lib/postcode.js";

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
