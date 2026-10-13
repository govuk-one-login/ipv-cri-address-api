import { logger } from "@govuk-one-login/cri-logger";
import { DynamoDBDocument, GetCommand, GetCommandOutput } from "@aws-sdk/lib-dynamodb";

export class DynamoDbService {
    constructor(private readonly dynamoDbClient: DynamoDBDocument) {}

    public async getItem(sessionId: string, tableName: string): Promise<GetCommandOutput> {
        try {
            const command = new GetCommand({
                TableName: tableName,
                Key: {
                    sessionId,
                },
            });

            const result = await this.dynamoDbClient.send(command);

            if (!result.Item) {
                logger.warn(`Could not find ${tableName} item with id: ${sessionId}`);
            }

            return result;
        } catch (error: unknown) {
            logger.error(`Error fetching item from ${tableName} for sessionId: ${sessionId}`, error as Error);

            throw new Error(`Error retrieving ${tableName} item with sessionId: ${sessionId}`, {
                cause: error,
            });
        }
    }
}
