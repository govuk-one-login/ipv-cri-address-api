import { describe, expect, it } from "vitest";
import { toCanonicalAddress } from "../../src/lib/ordnance-survey-response";

describe("toCanonicalAddress", () => {
    it("maps DPA fields to CanonicalAddress", () => {
        const result = toCanonicalAddress({
            UPRN: "123456",
            ORGANISATION_NAME: "HMRC",
            BUILDING_NUMBER: "1",
            THOROUGHFARE_NAME: "Parliament Street",
            POST_TOWN: "London",
            POSTCODE: "SW1A 2AA",
        });

        expect(result).toEqual({
            uprn: "123456",
            organisationName: "HMRC",
            buildingNumber: "1",
            streetName: "Parliament Street",
            addressLocality: "London",
            postalCode: "SW1A 2AA",
            addressCountry: "GB",
        });
    });

    it("always sets address country to GB", () => {
        const result = toCanonicalAddress({});

        expect(result.addressCountry).toBe("GB");
    });
});
