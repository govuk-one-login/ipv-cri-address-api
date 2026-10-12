import { describe, expect, it } from "vitest";
import {
    processOrdnanceSurveySuccessResponse,
    processOrdnanceSurveyBadResponse,
    processOrdnanceSurveyErrorResponse,
} from "../../src/lib/ordnance-survey-response";
import { ApiError } from "../../src/lib/error-handler";

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

    it("throws when the response body is empty", () => {
        expect(() => processOrdnanceSurveySuccessResponse("")).toThrow(SyntaxError);
    });

    it("throws when the response body contains only whitespace", () => {
        expect(() => processOrdnanceSurveySuccessResponse("   ")).toThrow(SyntaxError);
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
    it("throws a 404 ApiError containing the OS error message", () => {
        const responseBody = JSON.stringify({
            error: {
                statuscode: 500,
                message: "Unexpected error",
            },
        });
        let thrownError: unknown;

        try {
            processOrdnanceSurveyErrorResponse(responseBody);
        } catch (error) {
            thrownError = error;
        }

        expect(thrownError).toBeInstanceOf(ApiError);
        expect(thrownError).toMatchObject({
            message: "Error processing postcode lookup: Unexpected error",
            statusCode: 404,
        });
    });

    it("throws a 404 ApiError containing the raw body when the response is malformed", () => {
        let thrownError: unknown;

        try {
            processOrdnanceSurveyErrorResponse("{");
        } catch (error) {
            thrownError = error;
        }

        expect(thrownError).toBeInstanceOf(ApiError);
        expect(thrownError).toMatchObject({
            message: "Error processing postcode lookup: {",
            statusCode: 404,
        });
    });
});
