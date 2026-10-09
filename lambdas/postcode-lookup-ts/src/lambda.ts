import { Logger } from "@aws-lambda-powertools/logger";

import { DynamoDbClient } from "./lib/dynamo-db-client";
import { PostcodeLookupHandler } from "./postcode-lookup-handler";
import { PostcodeLookupService } from "./services/postcode-lookup-service";
import { SessionService } from "./services/session-service";

const logger = new Logger();

const sessionTableName = process.env.SESSION_TABLE;
const osApiUrl = process.env.OS_API_URL;
const osApiKey = process.env.OS_API_KEY;

const auditQueueUrl = process.env.SQS_AUDIT_EVENT_QUEUE_URL;
const componentId = process.env.VERIFIABLE_CREDENTIAL_ISSUER;

if (!sessionTableName) {
    throw new Error("SESSION_TABLE environment variable is required");
}

if (!osApiUrl) {
    throw new Error("OS_API_URL environment variable is required");
}

if (!osApiKey) {
    throw new Error("OS_API_KEY environment variable is required");
}

if (!auditQueueUrl) {
    throw new Error("SQS_AUDIT_EVENT_QUEUE_URL environment variable is required");
}

if (!componentId) {
    throw new Error("VERIFIABLE_CREDENTIAL_ISSUER environment variable is required");
}

const sessionService = new SessionService(DynamoDbClient, logger, sessionTableName);

const postcodeLookupService = new PostcodeLookupService(logger, fetch, osApiUrl, osApiKey);

const handlerClass = new PostcodeLookupHandler(postcodeLookupService, sessionService, {
    queueUrl: auditQueueUrl,
    componentId,
});

export const lambdaHandler = handlerClass.handler.bind(handlerClass);
