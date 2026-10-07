import { Logger } from "@aws-lambda-powertools/logger";
import { CanonicalAddress } from "../types/canonical-address";
import {
    processOrdnanceSurveySuccessResponse,
    processOrdnanceSurveyBadResponse,
    processOrdnanceSurveyErrorResponse,
} from "../lib/ordnance-survey-response";
import { ApiError } from "../lib/error-handler";

export class PostcodeLookupService {
    constructor(
        private readonly logger: Logger,
        private readonly fetchFn: typeof fetch = fetch,
    ) {}
    private buildLookupUrl(postcode: string): string {
        const url = new URL("https://api.os.uk/search/places/v1/postcode");
        url.searchParams.set("postcode", postcode);
        return url.toString();
    }
    public async lookupPostcode(postcode: string, _clientId: string): Promise<CanonicalAddress[]> {
        if (!postcode?.trim()) {
            throw new Error("Postcode must not be null or blank");
        }
        this.logger.info(`Looking up postcode: ${postcode}`);
        let response: Response;

        try {
            response = await this.fetchFn(this.buildLookupUrl(postcode), {
                headers: {
                    Accept: "application/json",
                    key: "test-api-key",
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
