import { Logger } from "@aws-lambda-powertools/logger";
import { CanonicalAddress } from "../types/canonical-address";
import {
    processOrdnanceSurveySuccessResponse,
    processOrdnanceSurveyBadResponse,
    processOrdnanceSurveyErrorResponse,
} from "../lib/ordnance-survey-response";
import { ApiError } from "../lib/error-handler";
import type { ConfigurationService } from "./configuration-service";

export class PostcodeLookupService {
    constructor(
        private readonly logger: Logger,
        private readonly configurationService: ConfigurationService,
        private readonly fetchFn: typeof fetch = fetch,
    ) {}
    private buildLookupUrl(postcode: string, osApiUrl: string): string {
        const url = new URL(osApiUrl);
        url.searchParams.set("postcode", decodeURIComponent(postcode));

        return url.toString();
    }
    public async lookupPostcode(postcode: string, clientId: string): Promise<CanonicalAddress[]> {
        if (!postcode?.trim()) {
            throw new ApiError("Postcode must not be null or blank", 400);
        }

        let osApiUrl: string;
        let osApiKey: string;

        try {
            osApiUrl = await this.configurationService.getParameterValue(`OrdnanceSurveyAPIUrl/${clientId}`);

            osApiKey = await this.configurationService.getSecretValue("OrdnanceSurveyAPIKey");
        } catch {
            throw new ApiError("The Client ID provided for this session is not supported", 400);
        }

        this.logger.info("Looking up postcode");

        let response: Response;

        let lookupUrl: string;

        try {
            lookupUrl = this.buildLookupUrl(postcode, osApiUrl);
        } catch {
            throw new ApiError("Error building URI for postcode lookup", 400);
        }

        try {
            response = await this.fetchFn(lookupUrl, {
                headers: {
                    Accept: "application/json",
                    key: osApiKey,
                },
            });
        } catch {
            throw new ApiError("Error sending request for postcode lookup", 500);
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
