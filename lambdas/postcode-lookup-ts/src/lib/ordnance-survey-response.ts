import { CriError } from "@govuk-one-login/cri-error-response";
import { logger } from "@govuk-one-login/cri-logger";

import { CanonicalAddress } from "../types/canonical-address.js";
import { Dpa, OrdnanceSurveyPostcodeResponse } from "../types/ordnance-survey.js";
import { sanitize } from "./pii-postcode-masker.js";

const isRecord = (value: unknown): value is Record<string, unknown> => typeof value === "object" && value !== null;

export const toCanonicalAddress = (dpa: Dpa): CanonicalAddress => ({
    uprn: dpa.UPRN ? Number(dpa.UPRN) : undefined,
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

export const processOrdnanceSurveyBadResponse = (responseBody: string): [] => {
    let parsedResponse: unknown;

    try {
        parsedResponse = JSON.parse(responseBody);
    } catch {
        logger.error(`Ordnance Survey Responded with unknown error: ${sanitize(responseBody)}`);
        return [];
    }

    const error = isRecord(parsedResponse) && isRecord(parsedResponse.error) ? parsedResponse.error : undefined;

    if (typeof error?.statuscode === "number" && typeof error.message === "string") {
        logger.error(`Ordnance Survey Responded with status ${error.statuscode}: ${sanitize(error.message)}`);
        return [];
    }

    logger.error(`Ordnance Survey Responded with unknown error: ${sanitize(responseBody)}`);
    return [];
};

export const processOrdnanceSurveyErrorResponse = (responseBody: string): never => {
    const sanitizedResponse = sanitize(responseBody);

    logger.error(`Ordnance Survey Responded with unknown error: ${sanitizedResponse}`);

    throw new CriError(404, `Error processing postcode lookup: ${sanitizedResponse}`);
};

export const processOrdnanceSurveySuccessResponse = (responseBody: string): CanonicalAddress[] => {
    const response = JSON.parse(responseBody) as OrdnanceSurveyPostcodeResponse;

    if (response.results === null || response.results === undefined) {
        logger.warn("Postcode lookup returned no results");
        return [];
    }

    return (
        response.results
            .map((result) => result.DPA)
            .filter((dpa): dpa is Dpa => dpa != null)
            .map(toCanonicalAddress) ?? []
    );
};
