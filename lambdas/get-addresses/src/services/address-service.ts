/* eslint-disable no-console */
import { DynamoDBDocument, GetCommand } from "@aws-sdk/lib-dynamodb";
import { CanonicalAddress } from "../types/canonical-address";

export class AddressService {
    constructor(
        private tableName: string | undefined,
        private dynamoDbClient: DynamoDBDocument,
    ) {}

    public async getAddressesBySessionId(sessionId: string | undefined): Promise<CanonicalAddress[]> {
        try {
            const params = new GetCommand({
                TableName: this.tableName,
                Key: {
                    sessionId: sessionId,
                },
            });
            const result = await this.dynamoDbClient.send(params);
            return result.Item ? (result.Item as unknown as CanonicalAddress[]) : [];
        } catch (e) {
            console.error("Error retrieving address item from dynamodb", e as Error);
            throw e;
        }
    }
}
