import { describe, expect, it } from "vitest";

import { redactObjectFields } from "../src/lib/pii-redactor.js";

describe("pii-redactor", () => {
    it("redacts all object field values", () => {
        expect(
            redactObjectFields({
                uprn: "72262801",
                buildingNumber: "8",
                streetName: "GRANGE FIELDS WAY",
                addressCountry: "GB",
                validFrom: "2010-00-00",
            }),
        ).toEqual({
            uprn: "********",
            buildingNumber: "*",
            streetName: "*****************",
            addressCountry: "**",
            validFrom: "**********",
        });
    });

    it("uses six characters for an empty value", () => {
        expect(
            redactObjectFields({
                buildingName: "",
            }),
        ).toEqual({
            buildingName: "******",
        });
    });

    it("does not expose nested field values", () => {
        const result = redactObjectFields({
            nested: {
                value: "should-not-be-visible",
            },
        });

        expect(JSON.stringify(result)).not.toContain("should-not-be-visible");
    });
});
