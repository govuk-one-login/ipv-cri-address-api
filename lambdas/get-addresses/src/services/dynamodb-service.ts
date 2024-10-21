import { Logger } from "@aws-lambda-powertools/logger";
import { DynamoDBDocument, GetCommand } from "@aws-sdk/lib-dynamodb";

export class DynamoDbService {
    constructor(
        private readonly dynamoDbClient: DynamoDBDocument,
        private readonly logger: Logger,
    ) {}

    public async getItem(sessionId: string, tableName: string): Promise<unknown> {
        try {
            const params = new GetCommand({
                TableName: tableName,
                Key: {
                    sessionId: sessionId,
                },
            });
            const result = await this.dynamoDbClient.send(params);

            if (!result.Item) {
                this.logger.warn(`Could not find ${tableName} item with id: ${sessionId}`);
            }
            return result.Item;
        } catch (error: unknown) {
            this.logger.error(`Error fetching item from ${tableName} for sessionId: ${sessionId}`, error as Error);

            throw new Error(`Error retrieving ${tableName} item with sessionId: ${sessionId}, due to ${error}`);
        }
    }

    public getLogger(): Logger {
        return this.logger;
    }
}
