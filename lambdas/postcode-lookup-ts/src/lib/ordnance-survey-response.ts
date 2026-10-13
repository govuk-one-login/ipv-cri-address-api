import type { Logger } from "@aws-lambda-powertools/logger";
import { CanonicalAddress } from "../types/canonical-address";
import { Dpa, OrdnanceSurveyPostcodeError, OrdnanceSurveyPostcodeResponse } from "../types/ordnance-survey";
import { ApiError } from "../lib/error-handler";
import { sanitize } from "./pii-postcode-masker";

const LOG_RESPONSE_PREFIX = "Ordnance Survey Responded with ";

const isRecord = (value: unknown): value is Record<string, unknown> => typeof value === "object" && value !== null;

export const toCanonicalAddress = (dpa: Dpa): CanonicalAddress => ({
    uprn: dpa.UPRN,
    organisationName: dpa.ORGANISATION_NAME,
    departmentName: dpa.DEPARTMENT_NAME,
    subBuildingName: dpa.SUB_BUILDING_NAME,
    buildingName: dpa.BUILDING_NAME,
    buildingNumber: dpa.BUILDING_NUMBER,
    dependentStreetName: dpa.DEPENDENT_THOROUGHFARE_NAME,
    streetName: dpa.THOROUGHFARE_NAME,
    doubleDependentAddressLocality: dpa.DOUBLE_DEPENDENT_LOCALITY,
    dependentAddressLocality: dpa.DEPENDENT_LOCALITY,
    addressLocality: dpa.POST_TOWN,
    postalCode: dpa.POSTCODE,
    addressCountry: "GB",
});

export const processOrdnanceSurveyBadResponse = (responseBody: string, logger: Pick<Logger, "error">): [] => {
    let parsedResponse: unknown;

    try {
        parsedResponse = JSON.parse(responseBody);
    } catch {
        logger.error(`${LOG_RESPONSE_PREFIX}unknown error: ${sanitize(responseBody)}`);
        return [];
    }

    const error = isRecord(parsedResponse) && isRecord(parsedResponse.error) ? parsedResponse.error : undefined;

    if (typeof error?.statuscode === "number" && typeof error.message === "string") {
        logger.error(`${LOG_RESPONSE_PREFIX}status ${error.statuscode}: ${sanitize(error.message)}`);
        return [];
    }

    logger.error(`${LOG_RESPONSE_PREFIX}unknown error: ${sanitize(responseBody)}`);
    return [];
};

export const processOrdnanceSurveyErrorResponse = (responseBody: string): never => {
    try {
        const error = JSON.parse(responseBody) as OrdnanceSurveyPostcodeError;

        throw new ApiError(`Error processing postcode lookup: ${error.error.message}`, 404);
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }

        throw new ApiError(`Error processing postcode lookup: ${responseBody}`, 404);
    }
};

export const processOrdnanceSurveySuccessResponse = (responseBody: string): CanonicalAddress[] => {
    const response = JSON.parse(responseBody) as OrdnanceSurveyPostcodeResponse;

    return (
        response.results
            ?.map((result) => result.DPA)
            .filter((dpa): dpa is Dpa => dpa != null)
            .map(toCanonicalAddress) ?? []
    );
};
