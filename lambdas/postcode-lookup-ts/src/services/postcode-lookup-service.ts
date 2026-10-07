import { Logger } from "@aws-lambda-powertools/logger";
import { CanonicalAddress } from "../types/canonical-address";
import {
    processOrdnanceSurveySuccessResponse,
    processOrdnanceSurveyBadResponse,
    processOrdnanceSurveyErrorResponse,
} from "../lib/ordnance-survey-response";

export class PostcodeLookupService {
    constructor(
        private readonly logger: Logger,
        private readonly fetchFn: typeof fetch = fetch,
    ) {}
    public async lookupPostcode(postcode: string, _clientId: string): Promise<CanonicalAddress[]> {
        if (!postcode?.trim()) {
            throw new Error("Postcode must not be null or blank");
        }
        this.logger.info(`Looking up postcode: ${postcode}`);

        const response = await this.fetchFn("some-url");

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
