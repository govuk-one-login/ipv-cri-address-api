import type { LambdaInterface } from "@aws-lambda-powertools/commons/types";
import { Logger } from "@aws-lambda-powertools/logger";
import type { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";

import { buildAndSendAuditEvent } from "@govuk-one-login/cri-audit";
import { captureMetric, captureMetricWithDimensions } from "@govuk-one-login/cri-metrics";

import { ApiError, handleError } from "./lib/error-handler";
import { getPostcodeErrorDimensions, POSTCODE_LOOKUP_ERROR } from "./lib/postcode-error-metrics";
import { getSessionId } from "./lib/session-header";
import { PostcodeLookupService } from "./services/postcode-lookup-service";
import { SessionService } from "./services/session-service";
import type { PostcodeRequest } from "./types/postcode-request";

const logger = new Logger();

const AUDIT_EVENT_TYPE = {
    REQUEST_SENT: "REQUEST_SENT",
    RESPONSE_RECEIVED: "RESPONSE_RECEIVED",
} as const;

const POSTCODE_LOOKUP_METRIC = "postcode_lookup";
const TXMA_AUDIT_ENCODED_HEADER = "txma-audit-encoded";

export interface AuditConfig {
    queueUrl: string;
    componentId: string;
}

export class PostcodeLookupHandler implements LambdaInterface {
    constructor(
        private readonly postcodeLookupService: PostcodeLookupService,
        private readonly sessionService: SessionService,
        private readonly auditConfig: AuditConfig,
    ) {}

    public async handler(event: APIGatewayProxyEvent, context: Context): Promise<APIGatewayProxyResult> {
        try {
            const postcode = this.getPostcodeFromRequest(event);
            const sessionId = getSessionId(event.headers);
            const session = await this.sessionService.validateSessionId(sessionId);
            logger.info("found session");
            const restricted = this.buildAuditRestricted(postcode, event.headers);

            await buildAndSendAuditEvent(
                this.auditConfig.queueUrl,
                AUDIT_EVENT_TYPE.REQUEST_SENT,
                this.auditConfig.componentId,
                session,
                { restricted },
            );

            const results = await this.postcodeLookupService.lookupPostcode(postcode, session.clientId);

            captureMetric(POSTCODE_LOOKUP_METRIC);

            await buildAndSendAuditEvent(
                this.auditConfig.queueUrl,
                AUDIT_EVENT_TYPE.RESPONSE_RECEIVED,
                this.auditConfig.componentId,
                session,
                { restricted },
            );

            return {
                statusCode: 200,
                body: JSON.stringify(results),
            };
        } catch (error: unknown) {
            captureMetricWithDimensions(POSTCODE_LOOKUP_ERROR, getPostcodeErrorDimensions(error));

            return handleError(logger, error, `Error in ${context.functionName}`);
        }
    }

    private getPostcodeFromRequest(event: APIGatewayProxyEvent): string {
        if (!event.body) {
            throw new ApiError("Missing postcode in request body", 400);
        }

        let request: PostcodeRequest;

        try {
            request = JSON.parse(event.body) as PostcodeRequest;
        } catch {
            throw new ApiError("Failed to parse postcode from request body", 400);
        }

        if (!request.postcode?.trim()) {
            throw new ApiError("Missing postcode in request body", 400);
        }

        return request.postcode;
    }

    private buildAuditRestricted(postcode: string, headers: APIGatewayProxyEvent["headers"]): Record<string, unknown> {
        const encodedDeviceInformation = headers[TXMA_AUDIT_ENCODED_HEADER];

        return {
            addresses: [
                {
                    postalCode: decodeURIComponent(postcode).toUpperCase(),
                },
            ],
            ...(encodedDeviceInformation && {
                device_information: {
                    encoded: encodedDeviceInformation,
                },
            }),
        };
    }
}
