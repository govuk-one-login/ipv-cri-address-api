import { logger } from "@govuk-one-login/cri-logger";
import { DynamoDBDocument, GetCommand, GetCommandOutput, UpdateCommand } from "@aws-sdk/lib-dynamodb";

export const getItem = async (
    dynamoDbClient: DynamoDBDocument,
    sessionId: string,
    tableName: string,
): Promise<GetCommandOutput> => {
    try {
        const command = new GetCommand({
            TableName: tableName,
            Key: {
                sessionId,
            },
        });

        const result = await dynamoDbClient.send(command);

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
};
export const updateAuthorizationCode = async (
    dynamoDbClient: DynamoDBDocument,
    sessionId: string,
    authorizationCode: string,
    authorizationCodeExpiryDate: number,
    tableName: string,
): Promise<void> => {
    try {
        const command = new UpdateCommand({
            TableName: tableName,
            Key: {
                sessionId,
            },
            UpdateExpression:
                "SET authorizationCode = :authorizationCode, authorizationCodeExpiryDate = :authorizationCodeExpiryDate",
            ConditionExpression: "attribute_exists(sessionId)",
            ExpressionAttributeValues: {
                ":authorizationCode": authorizationCode,
                ":authorizationCodeExpiryDate": authorizationCodeExpiryDate,
            },
        });

        await dynamoDbClient.send(command);
    } catch (error: unknown) {
        logger.error(`Error updating authorization code in ${tableName} for sessionId: ${sessionId}`, error as Error);

        throw new Error(`Error updating ${tableName} item with sessionId: ${sessionId}`, { cause: error });
    }
};
