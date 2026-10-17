import { beforeEach, describe, expect, it, vi } from "vitest";
import { CriError } from "@govuk-one-login/cri-error-response";
import { logger } from "@govuk-one-login/cri-logger";
import {
    processOrdnanceSurveySuccessResponse,
    processOrdnanceSurveyBadResponse,
    processOrdnanceSurveyErrorResponse,
} from "../../src/lib/ordnance-survey-response.js";

describe("processOrdnanceSurveySuccessResponse", () => {
    const loggerWarn = vi.spyOn(logger, "warn");

    beforeEach(() => {
        loggerWarn.mockClear();
    });

    it("returns mapped canonical addresses", () => {
        const response = JSON.stringify({
            results: [
                {
                    DPA: {
                        UPRN: 123,
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
                uprn: 123,
                postalCode: "SW1A 2AA",
                addressLocality: "London",
                addressCountry: "GB",
            }),
        );
    });

    it("returns an empty array without warning when results is empty", () => {
        const result = processOrdnanceSurveySuccessResponse(
            JSON.stringify({
                results: [],
            }),
        );

        expect(result).toEqual([]);
        expect(loggerWarn).not.toHaveBeenCalled();
    });

    it("returns an empty array and logs a warning when results is missing", () => {
        const result = processOrdnanceSurveySuccessResponse("{}");

        expect(result).toEqual([]);
        expect(loggerWarn).toHaveBeenCalledOnce();
        expect(loggerWarn).toHaveBeenCalledWith("Postcode lookup returned no results");
    });

    it("returns an empty array and logs a warning when results is null", () => {
        const result = processOrdnanceSurveySuccessResponse(
            JSON.stringify({
                results: null,
            }),
        );

        expect(result).toEqual([]);
        expect(loggerWarn).toHaveBeenCalledOnce();
        expect(loggerWarn).toHaveBeenCalledWith("Postcode lookup returned no results");
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

    it("converts UPRN from string to number", () => {
        const response = JSON.stringify({
            results: [
                {
                    DPA: {
                        UPRN: "100023336956",
                        POSTCODE: "SW1A 2AA",
                        POST_TOWN: "London",
                    },
                },
            ],
        });

        const result = processOrdnanceSurveySuccessResponse(response);

        expect(result[0].uprn).toBe(100023336956);
        expect(typeof result[0].uprn).toBe("number");
    });

    it("does not partially parse an invalid UPRN", () => {
        const response = JSON.stringify({
            results: [
                {
                    DPA: {
                        UPRN: "5AB",
                        POSTCODE: "SW1A 2AA",
                        POST_TOWN: "London",
                    },
                },
            ],
        });

        const result = processOrdnanceSurveySuccessResponse(response);

        expect(result[0].uprn).toBeNaN();
    });

    it("leaves UPRN undefined when it is missing", () => {
        const response = JSON.stringify({
            results: [
                {
                    DPA: {
                        POSTCODE: "SW1A 2AA",
                        POST_TOWN: "London",
                    },
                },
            ],
        });

        const result = processOrdnanceSurveySuccessResponse(response);

        expect(result[0].uprn).toBeUndefined();
    });

    it("leaves UPRN undefined when it is empty", () => {
        const response = JSON.stringify({
            results: [
                {
                    DPA: {
                        UPRN: "",
                        POSTCODE: "SW1A 2AA",
                        POST_TOWN: "London",
                    },
                },
            ],
        });

        const result = processOrdnanceSurveySuccessResponse(response);

        expect(result[0].uprn).toBeUndefined();
    });
});

describe("processOrdnanceSurveyBadResponse", () => {
    const loggerError = vi.spyOn(logger, "error");

    beforeEach(() => {
        loggerError.mockClear();
    });

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
    const loggerError = vi.spyOn(logger, "error");

    beforeEach(() => {
        loggerError.mockClear();
    });

    it("throws a 404 CriError containing the sanitised OS error message", () => {
        const responseBody = JSON.stringify({
            error: {
                statuscode: 500,
                message: "Unexpected error for SW1A 1AA",
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
            message: `Error processing postcode lookup: ${JSON.stringify({
                error: {
                    statuscode: 500,
                    message: "Unexpected error for ********",
                },
            })}`,
            statusCode: 404,
        });
        expect((thrownError as Error).message).not.toContain("SW1A 1AA");
    });

    it("logs the sanitised OS error without exposing the postcode", () => {
        const responseBody = JSON.stringify({
            error: {
                statuscode: 500,
                message: "Unexpected error for SW1A 1AA",
            },
        });

        expect(() => processOrdnanceSurveyErrorResponse(responseBody)).toThrow(CriError);

        expect(loggerError).toHaveBeenCalledWith(
            `Ordnance Survey Responded with unknown error: ${JSON.stringify({
                error: {
                    statuscode: 500,
                    message: "Unexpected error for ********",
                },
            })}`,
        );
        expect(loggerError).not.toHaveBeenCalledWith(expect.stringContaining("SW1A 1AA"));
    });

    it("uses the sanitised full OS response in the processing error", () => {
        const responseBody = JSON.stringify({
            error: {
                statuscode: 500,
                message: "Unexpected error for SW1A 1AA",
            },
        });

        expect(() => processOrdnanceSurveyErrorResponse(responseBody)).toThrow(
            `Error processing postcode lookup: ${JSON.stringify({
                error: {
                    statuscode: 500,
                    message: "Unexpected error for ********",
                },
            })}`,
        );
    });

    it("throws a 404 CriError containing the sanitised raw body when the response is malformed", () => {
        const responseBody = "Invalid response for SW1A 1AA";

        let thrownError: unknown;

        try {
            processOrdnanceSurveyErrorResponse(responseBody);
        } catch (error) {
            thrownError = error;
        }

        expect(thrownError).toBeInstanceOf(CriError);
        expect(thrownError).toMatchObject({
            message: "Error processing postcode lookup: Invalid response for ********",
            statusCode: 404,
        });
        expect((thrownError as Error).message).not.toContain("SW1A 1AA");
    });

    it("logs the sanitised raw body when the response is malformed", () => {
        const responseBody = "Invalid response for SW1A 1AA";

        expect(() => processOrdnanceSurveyErrorResponse(responseBody)).toThrow(CriError);

        expect(loggerError).toHaveBeenCalledWith(
            "Ordnance Survey Responded with unknown error: Invalid response for ********",
        );
        expect(loggerError).not.toHaveBeenCalledWith(expect.stringContaining("SW1A 1AA"));
    });

    it("does not include a postcode in the thrown error for malformed JSON", () => {
        const responseBody = '{"error":"Failed for postcode SW1A 1AA"';

        let thrownError: unknown;

        try {
            processOrdnanceSurveyErrorResponse(responseBody);
        } catch (error) {
            thrownError = error;
        }

        expect(thrownError).toBeInstanceOf(CriError);
        expect((thrownError as Error).message).toBe(
            'Error processing postcode lookup: {"error":"Failed for postcode ********"',
        );
        expect((thrownError as Error).message).not.toContain("SW1A 1AA");
    });
});
