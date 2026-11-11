import type { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";

import { CriError, formatErrorResponse } from "@govuk-one-login/cri-error-response";
import { logger } from "@govuk-one-login/cri-logger";
import { captureMetric } from "@govuk-one-login/cri-metrics";
import type { SessionItem } from "@govuk-one-login/cri-types";

import { getSessionId } from "../../common/src/lib/session-header.js";
import type { CanonicalAddress } from "../../common/src/types/canonical-address.js";

const SAVE_ADDRESS_METRIC_NAME = "address";

const SESSION_ERROR_MESSAGES = new Set(["Session not found", "Session expired"]);

interface HandlerDependencies {
    parseAddresses: (body: string | null) => CanonicalAddress[];

    getSessionById: (sessionId: string) => Promise<SessionItem>;

    applyAddressValidity: (addresses: CanonicalAddress[]) => CanonicalAddress[];

    getSessionExpirationEpoch: () => number;

    saveAddresses: (sessionId: string, addresses: CanonicalAddress[], expiryDate: number) => Promise<void>;

    storeAddressEntryTypeMetrics: (addresses: CanonicalAddress[]) => void;

    createAuthorizationCode: (session: SessionItem) => Promise<void>;
}

export const createHandler =
    ({
        parseAddresses,
        getSessionById,
        applyAddressValidity,
        getSessionExpirationEpoch,
        saveAddresses,
        storeAddressEntryTypeMetrics,
        createAuthorizationCode,
    }: HandlerDependencies) =>
    async (event: APIGatewayProxyEvent, _context: Context): Promise<APIGatewayProxyResult> => {
        try {
            const addresses = parseAddresses(event.body);

            if (addresses.length === 0) {
                logger.info("No addresses provided");
                return {
                    statusCode: 200,
                    body: "",
                };
            }
            const sessionId = getSessionId(event.headers);
            const session = await getSessionById(sessionId);

            logger.info("found session");

            const addressesWithValidity = applyAddressValidity(addresses);

            const expiryDate = getSessionExpirationEpoch();

            await saveAddresses(sessionId, addressesWithValidity, expiryDate);
            logger.info("Addresses saved");

            storeAddressEntryTypeMetrics(addressesWithValidity);

            await createAuthorizationCode(session);

            captureMetric(SAVE_ADDRESS_METRIC_NAME);

            return {
                statusCode: 204,
                body: "",
            };
        } catch (error: unknown) {
            captureMetric(SAVE_ADDRESS_METRIC_NAME, 0);

            return buildErrorResponse(error);
        }
    };

const buildErrorResponse = (error: unknown): APIGatewayProxyResult => {
    if (error instanceof CriError && error.statusCode === 403 && SESSION_ERROR_MESSAGES.has(error.message)) {
        logger.error("Address session validation failed");
        return {
            statusCode: 403,
            body: JSON.stringify({
                error: "access_denied",
                error_description: "Access denied by resource owner or " + `authorization server - ${error.message}`,
            }),
        };
    }
    return formatErrorResponse(error);
};
