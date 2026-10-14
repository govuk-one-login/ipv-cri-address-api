import { DynamoDbClient } from "./lib/dynamo-db-client.js";
import { createHandler } from "./postcode-lookup-handler.js";
import { getParameterValue, getSecretValue } from "./services/configuration-service.js";
import { lookupPostcode } from "./services/postcode-lookup-service.js";
import { validateSessionId } from "./services/session-service.js";
const sessionTableName = process.env.SESSION_TABLE;
import type { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";

import type { LambdaInterface } from "@aws-lambda-powertools/commons/types";

import { logger } from "@govuk-one-login/cri-logger";
import { metrics } from "@govuk-one-login/cri-metrics";
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

const postcodeLookupHandler = createHandler({
    queueUrl: auditQueueUrl,
    componentId,
    validateSessionId: (sessionId) =>
        validateSessionId(sessionId, {
            dynamoDbClient: DynamoDbClient,
            sessionTableName,
        }),
    lookupPostcode: (postcode, clientId) =>
        lookupPostcode(postcode, clientId, {
            stackName,
            secretPrefix,
            getParameterValue,
            getSecretValue,
        }),
});

class PostcodeLookupLambda implements LambdaInterface {
    @logger.injectLambdaContext({ resetKeys: true })
    @metrics.logMetrics({
        throwOnEmptyMetrics: false,
        captureColdStartMetric: true,
    })
    public async handler(event: APIGatewayProxyEvent, context: Context): Promise<APIGatewayProxyResult> {
        return postcodeLookupHandler(event, context);
    }
}

const handlerClass = new PostcodeLookupLambda();

export const lambdaHandler = handlerClass.handler.bind(handlerClass);
