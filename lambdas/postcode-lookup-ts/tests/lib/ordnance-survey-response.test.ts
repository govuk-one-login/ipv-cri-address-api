import { describe, expect, it } from "vitest";
import {
    processOrdnanceSurveySuccessResponse,
    processOrdnanceSurveyBadResponse,
    processOrdnanceSurveyErrorResponse,
} from "../../src/lib/ordnance-survey-response";

describe("processOrdnanceSurveySuccessResponse", () => {
    it("returns mapped canonical addresses", () => {
        const response = JSON.stringify({
            results: [
                {
                    DPA: {
                        UPRN: "123",
                        POSTCODE: "SW1A 2AA",
                        POST_TOWN: "London",
                    },
                },
            ],
        });

        const result = processOrdnanceSurveySuccessResponse(response);

        expect(result).toHaveLength(1);
        expect(result[0]).toEqual(
            expect.objectContaining({
                uprn: "123",
                postalCode: "SW1A 2AA",
                addressLocality: "London",
                addressCountry: "GB",
            }),
        );
    });

    it("returns an empty array when results is empty", () => {
        const result = processOrdnanceSurveySuccessResponse(
            JSON.stringify({
                results: [],
            }),
        );

        expect(result).toEqual([]);
    });

    it("returns an empty array when results is missing", () => {
        const result = processOrdnanceSurveySuccessResponse("{}");

        expect(result).toEqual([]);
    });

    it("returns an empty array when the response body is empty", () => {
        const result = processOrdnanceSurveySuccessResponse("");

        expect(result).toEqual([]);
    });

    it("returns an empty array when the response body contains only whitespace", () => {
        const result = processOrdnanceSurveySuccessResponse("   ");

        expect(result).toEqual([]);
    });

    it("ignores results without a DPA", () => {
        const result = processOrdnanceSurveySuccessResponse(
            JSON.stringify({
                results: [{}],
            }),
        );

        expect(result).toEqual([]);
    });
});

describe("processOrdnanceSurveyBadResponse", () => {
    it("returns an empty array for OS bad requests", () => {
        expect(
            processOrdnanceSurveyBadResponse(
                JSON.stringify({
                    error: {
                        statuscode: 400,
                        message: "Bad request",
                    },
                }),
            ),
        ).toEqual([]);
    });

    it("returns an empty array for malformed responses", () => {
        expect(processOrdnanceSurveyBadResponse("{")).toEqual([]);
    });
});

describe("processOrdnanceSurveyErrorResponse", () => {
    it("throws when the OS service returns an error", () => {
        expect(() =>
            processOrdnanceSurveyErrorResponse(
                JSON.stringify({
                    error: {
                        statuscode: 500,
                        message: "Unexpected error",
                    },
                }),
            ),
        ).toThrow();
    });

    it("throws when the response is malformed", () => {
        expect(() => processOrdnanceSurveyErrorResponse("{")).toThrow();
    });
});
