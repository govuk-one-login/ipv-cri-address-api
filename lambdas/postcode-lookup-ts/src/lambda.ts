import { Logger } from "@aws-lambda-powertools/logger";

import { DynamoDbClient } from "./lib/dynamo-db-client";
import { PostcodeLookupHandler } from "./postcode-lookup-handler";
import { PostcodeLookupService } from "./services/postcode-lookup-service";
import { SessionService } from "./services/session-service";
import { PowertoolsConfigurationService } from "./services/powertools-configuration-service";
const logger = new Logger();

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

const configurationService = new PowertoolsConfigurationService(stackName, secretPrefix);

const sessionService = new SessionService(DynamoDbClient, logger, sessionTableName);

const postcodeLookupService = new PostcodeLookupService(logger, configurationService);

const handlerClass = new PostcodeLookupHandler(postcodeLookupService, sessionService, {
    queueUrl: auditQueueUrl,
    componentId,
});

export const lambdaHandler = handlerClass.handler.bind(handlerClass);
