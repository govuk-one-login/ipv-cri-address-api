import { CriError } from "@govuk-one-login/cri-error-response";
import { logger } from "@govuk-one-login/cri-logger";

import { CanonicalAddress } from "../types/canonical-address";
import {
    processOrdnanceSurveySuccessResponse,
    processOrdnanceSurveyBadResponse,
    processOrdnanceSurveyErrorResponse,
} from "../lib/ordnance-survey-response";
import type { ConfigurationService } from "./configuration-service";

export class PostcodeLookupService {
    constructor(
        private readonly configurationService: ConfigurationService,
        private readonly fetchFn: typeof fetch = fetch,
    ) {}
    private buildLookupUrl(postcode: string, osApiUrl: string): string {
        const url = new URL(osApiUrl);
        const decodedPostcode = decodeURIComponent(postcode.replace(/\+/g, " "));
        url.searchParams.set("postcode", decodedPostcode);

        return url.toString();
    }
    public async lookupPostcode(postcode: string, clientId: string): Promise<CanonicalAddress[]> {
        if (!postcode?.trim()) {
            throw new CriError(400, "Postcode must not be null or blank");
        }

        let osApiUrl: string;

        try {
            osApiUrl = await this.configurationService.getParameterValue(`OrdnanceSurveyAPIUrl/${clientId}`);
        } catch {
            throw new CriError(400, "The Client ID provided for this session is not supported");
        }

        let osApiKey: string;

        try {
            osApiKey = await this.configurationService.getSecretValue("OrdnanceSurveyAPIKey");
        } catch {
            throw new CriError(500, "Unable to retrieve the Ordnance Survey API key");
        }

        logger.info("Looking up postcode");

        let response: Response;

        let lookupUrl: string;

        try {
            lookupUrl = this.buildLookupUrl(postcode, osApiUrl);
        } catch {
            throw new CriError(400, "Error building URI for postcode lookup");
        }

        try {
            response = await this.fetchFn(lookupUrl, {
                headers: {
                    Accept: "application/json",
                    key: osApiKey,
                },
            });
        } catch {
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
    }
}
