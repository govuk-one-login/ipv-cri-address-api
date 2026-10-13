import { DynamoDbClient } from "./lib/dynamo-db-client";
import { createHandler } from "./postcode-lookup-handler";
import { getParameterValue, getSecretValue } from "./services/configuration-service";
import { lookupPostcode } from "./services/postcode-lookup-service";
import { validateSessionId } from "./services/session-service";
const sessionTableName = process.env.SESSION_TABLE;

const stackName = process.env.AWS_STACK_NAME;

const secretPrefix = process.env.SECRET_PREFIX;

const auditQueueUrl = process.env.SQS_AUDIT_EVENT_QUEUE_URL;

const componentId = process.env.VERIFIABLE_CREDENTIAL_ISSUER;

if (!sessionTableName) {
    throw new Error("SESSION_TABLE environment variable is required");
}

if (!stackName) {
    throw new Error("AWS_STACK_NAME environment variable is required");
}

if (!secretPrefix) {
    throw new Error("SECRET_PREFIX environment variable is required");
}

if (!auditQueueUrl) {
    throw new Error("SQS_AUDIT_EVENT_QUEUE_URL environment variable is required");
}

if (!componentId) {
    throw new Error("VERIFIABLE_CREDENTIAL_ISSUER environment variable is required");
}

export const lambdaHandler = createHandler({
    queueUrl: auditQueueUrl,
    componentId,
    validateSessionId: (sessionId) =>
        validateSessionId(sessionId, { dynamoDbClient: DynamoDbClient, sessionTableName }),
    lookupPostcode: (postcode, clientId) =>
        lookupPostcode(postcode, clientId, {
            stackName,
            secretPrefix,
            getParameterValue,
            getSecretValue,
        }),
});
