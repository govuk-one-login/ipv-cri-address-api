/* eslint-disable no-console */
import { DynamoDBDocument, GetCommand } from "@aws-sdk/lib-dynamodb";

export class AddressService {
    constructor(private tableName: string, private dynamoDbClient: DynamoDBDocument) {}

    public async getAddressesBySessionId(sessionId: string | undefined): Promise<any> {
        try {
            console.log(`TableName: ${this.tableName}\nsessionId: ${sessionId}`);

            const params = new GetCommand({
                TableName: this.tableName,
                Key: {
                    sessionId: sessionId,
                },
            });
            const result = await this.dynamoDbClient.send(params);
            return result.Item ?? [];
        } catch (e) {
            console.error("Error retrieving address item from dynamodb", e as Error);
            throw e;
        }
    }
}
