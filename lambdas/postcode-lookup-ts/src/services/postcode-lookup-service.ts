import { Logger } from "@aws-lambda-powertools/logger";
import { CanonicalAddress } from "../types/canonical-address";

export class PostcodeLookupService {
    constructor(private readonly logger: Logger) {}

    public async lookupPostcode(postcode: string, _clientId: string): Promise<CanonicalAddress[]> {
        this.logger.info(`Looking up postcode: ${postcode}`);

        return [];
    }
}
