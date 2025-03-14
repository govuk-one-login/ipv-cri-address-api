import { DynamoDBDocument } from "@aws-sdk/lib-dynamodb";
import { DynamoDbService } from "./dynamodb-service";
import { Logger } from "@aws-lambda-powertools/logger";
import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { Address } from "../types/address";
import { CanonicalAddress } from "../types/canonical-address";
import { SessionItem } from "../types/session";

const parameterPrefix = process.env.AWS_STACK_NAME || "";
const commonParameterPrefix = process.env.COMMON_PARAMETER_NAME_PREFIX || "";
export class AddressService {
    private readonly dbService: DynamoDbService;
    private readonly logger: Logger;

    constructor(dynamoDbClient: DynamoDBDocument, logger: Logger) {
        this.dbService = new DynamoDbService(dynamoDbClient, logger);
        this.logger = logger;
    }

    public async getAddressesBySessionId(sessionId: string): Promise<Address> {
        try {
            const [addressLookupTableName, sessionTableName] = await Promise.all([
                getParameter(`/${parameterPrefix}/AddressLookupTableName`),
                getParameter(`/${commonParameterPrefix}/SessionTableName`),
            ]);

            const sessionItem = (await this.dbService.getItem(sessionId, sessionTableName as string))
                .Item as SessionItem;

            this.logger.appendKeys({ govuk_signin_journey_id: sessionItem?.clientSessionId });
            this.logger.info(`Found session with context: ${sessionItem?.context}`);

            const result = await this.dbService.getItem(sessionId, addressLookupTableName as string);
            const canonicalAddresses = result?.Item?.addresses ? (result?.Item?.addresses as CanonicalAddress[]) : [];

            return {
                ...(sessionItem?.context && { context: sessionItem?.context }),
                addresses: canonicalAddresses,
            };
        } catch (error: unknown) {
            this.logger.error(`Failed to retrieve addresses for session ID: ${sessionId}`, error as Error);
            throw error;
        }
    }
}
