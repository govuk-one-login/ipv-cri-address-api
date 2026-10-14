import { CriError } from "@govuk-one-login/cri-error-response";
import { logger } from "@govuk-one-login/cri-logger";

import { CanonicalAddress } from "../types/canonical-address";
import {
    processOrdnanceSurveySuccessResponse,
    processOrdnanceSurveyBadResponse,
    processOrdnanceSurveyErrorResponse,
} from "../lib/ordnance-survey-response";

import { getParameterValue, getSecretValue } from "./configuration-service";

export interface PostcodeLookupDependencies {
    stackName: string;
    secretPrefix: string;
    getParameterValue?: typeof getParameterValue;
    getSecretValue?: typeof getSecretValue;
    fetchFn?: typeof fetch;
}

const buildLookupUrl = (postcode: string, osApiUrl: string): string => {
    const url = new URL(osApiUrl);
    const decodedPostcode = decodeURIComponent(postcode.replace(/\+/g, " "));
    url.searchParams.set("postcode", decodedPostcode);

    return url.toString();
};

export const lookupPostcode = async (
    postcode: string,
    clientId: string,
    {
        stackName,
        secretPrefix,
        getParameterValue: readParameter = getParameterValue,
        getSecretValue: readSecret = getSecretValue,
        fetchFn = fetch,
    }: PostcodeLookupDependencies,
): Promise<CanonicalAddress[]> => {
    if (!postcode?.trim()) {
        throw new CriError(400, "Postcode must not be null or blank");
    }

    let osApiUrl: string;

    try {
        osApiUrl = await readParameter(stackName, `OrdnanceSurveyAPIUrl/${clientId}`);
    } catch {
        throw new CriError(400, "The Client ID provided for this session is not supported");
    }

    let osApiKey: string;

    try {
        osApiKey = await readSecret(secretPrefix, "OrdnanceSurveyAPIKey");
    } catch {
        throw new CriError(500, "Unable to retrieve the Ordnance Survey API key");
    }

    logger.info("Looking up postcode");

    let lookupUrl: string;

    try {
        lookupUrl = buildLookupUrl(postcode, osApiUrl);
    } catch {
        throw new CriError(400, "Error building URI for postcode lookup");
    }

    let response: Response;

    try {
        response = await fetchFn(lookupUrl, {
            signal: AbortSignal.timeout(15_000),
            headers: {
                Accept: "application/json",
                key: osApiKey,
            },
        });
    } catch (error) {
        if (error instanceof DOMException && (error.name === "TimeoutError" || error.name === "AbortError")) {
            throw new CriError(408, "Error timed out waiting for postcode lookup response");
        }
        throw new CriError(404, "Error sending request for postcode lookup");
    }

    const responseBody = await response.text();

    switch (response.status) {
        case 200:
            return processOrdnanceSurveySuccessResponse(responseBody);

        case 400:
            return processOrdnanceSurveyBadResponse(responseBody);

        case 404:
            return [];

        default:
            return processOrdnanceSurveyErrorResponse(responseBody);
    }
};
