import type { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";

import type { LambdaInterface } from "@aws-lambda-powertools/commons/types";
import { logger } from "@govuk-one-login/cri-logger";
import { metrics } from "@govuk-one-login/cri-metrics";

import { DynamoDbClient } from "../../common/src/lib/dynamo-db-client.js";
import { createHandler } from "./address-handler.js";

import {
    parseAddresses,
    saveAddresses,
    applyAddressValidity,
    storeAddressEntryTypeMetrics,
} from "./services/address-service.js";

import { createAuthorizationCode, getSessionById } from "../../common/src/services/session-service.js";

import {
    getAuthorizationCodeExpirationEpoch,
    getSessionExpirationEpoch,
} from "../../common/src/services/configuration-service.js";

const addressTableName = process.env.ADDRESS_TABLE;
const sessionTableName = process.env.SESSION_TABLE;

if (!addressTableName) {
    throw new Error("ADDRESS_TABLE environment variable is required");
}

if (!sessionTableName) {
    throw new Error("SESSION_TABLE environment variable is required");
}

const addressHandler = createHandler({
    parseAddresses,

    getSessionById: (sessionId) =>
        getSessionById(sessionId, {
            dynamoDbClient: DynamoDbClient,
            sessionTableName,
        }),

    applyAddressValidity,

    getSessionExpirationEpoch,

    saveAddresses: (sessionId, addresses, expiryDate) =>
        saveAddresses(sessionId, addresses, expiryDate, {
            dynamoDbClient: DynamoDbClient,
            addressTableName,
        }),

    storeAddressEntryTypeMetrics,

    createAuthorizationCode: (session) =>
        createAuthorizationCode(session, {
            dynamoDbClient: DynamoDbClient,
            sessionTableName,
            getAuthorizationCodeExpirationEpoch,
        }),
});

class AddressLambda implements LambdaInterface {
    @logger.injectLambdaContext({
        resetKeys: true,
    })
    @metrics.logMetrics({
        throwOnEmptyMetrics: false,
        captureColdStartMetric: true,
    })
    public async handler(event: APIGatewayProxyEvent, context: Context): Promise<APIGatewayProxyResult> {
        return addressHandler(event, context);
    }
}

const handlerClass = new AddressLambda();

export const lambdaHandler = handlerClass.handler.bind(handlerClass);
