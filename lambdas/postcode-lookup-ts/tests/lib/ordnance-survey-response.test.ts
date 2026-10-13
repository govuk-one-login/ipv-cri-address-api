import { describe, expect, it, vi } from "vitest";
import { CriError } from "@govuk-one-login/cri-error-response";
import { logger } from "@govuk-one-login/cri-logger";
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
    const loggerError = vi.spyOn(logger, "error");

    it("returns an empty array and logs sanitized OS bad request details", () => {
        expect(
            processOrdnanceSurveyBadResponse(
                JSON.stringify({
                    error: {
                        statuscode: 400,
                        message:
                            "Requested postcode must contain a minimum of the sector plus 1 digit of the district e.g. SO1. Requested postcode was 5WF12LZ",
                    },
                }),
            ),
        ).toEqual([]);
        expect(loggerError).toHaveBeenCalledWith(
            "Ordnance Survey Responded with status 400: Requested postcode must contain a minimum of the sector plus 1 digit of the district e.g. SO1. Requested postcode was *******",
        );
    });

    it.each([
        { body: "invalid response for SW1A 1AA", sanitizedBody: "invalid response for ********" },
        { body: "", sanitizedBody: "" },
    ])("returns an empty array and logs sanitized unknown errors for body %j", ({ body, sanitizedBody }) => {
        expect(processOrdnanceSurveyBadResponse(body)).toEqual([]);
        expect(loggerError).toHaveBeenCalledWith(`Ordnance Survey Responded with unknown error: ${sanitizedBody}`);
    });
});

describe("processOrdnanceSurveyErrorResponse", () => {
    it("throws a 404 CriError containing the OS error message", () => {
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

        expect(thrownError).toBeInstanceOf(CriError);
        expect(thrownError).toMatchObject({
            message: "Error processing postcode lookup: Unexpected error",
            statusCode: 404,
        });
    });

    it("throws a 404 CriError containing the raw body when the response is malformed", () => {
        let thrownError: unknown;

        try {
            processOrdnanceSurveyErrorResponse("{");
        } catch (error) {
            thrownError = error;
        }

        expect(thrownError).toBeInstanceOf(CriError);
        expect(thrownError).toMatchObject({
            message: "Error processing postcode lookup: {",
            statusCode: 404,
        });
    });
});
