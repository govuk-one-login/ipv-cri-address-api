import type { LambdaInterface } from "@aws-lambda-powertools/commons/types";
import {
    assertFunctionResultWithTimeout,
    type AssertFunctionResultOutput,
    type HealthcheckMode,
    type HealthcheckValidationResult,
} from "@govuk-one-login/cri-healthcheck-assertion";
import { CriError, formatErrorResponse } from "@govuk-one-login/cri-error-response";
import { logger } from "@govuk-one-login/cri-logger";
import { metrics } from "@govuk-one-login/cri-metrics";
import type { APIGatewayProxyEvent, APIGatewayProxyResult, Context } from "aws-lambda";

import { getParameterValue, getSecretValue } from "../../postcode-lookup-ts/src/services/configuration-service.js";
import { processOrdnanceSurveySuccessResponse } from "../../postcode-lookup-ts/src/lib/ordnance-survey-response.js";
import type { CanonicalAddress } from "../../postcode-lookup-ts/src/types/canonical-address.js";

const TEST_POSTCODE = "SW1A 2AA";
const TEST_BUILDING_NUMBER = "10";
const TEST_STREET_NAME = "DOWNING STREET";

type HealthcheckConfig = {
    osApiUrl: string;
    osApiKey: string;
};

type EnvironmentVariables = {
    stackName: string;
    secretPrefix: string;
    clientId: string;
    timeoutMs: number;
    expectedApiKeyLength: number;
};

type ReportedConfig = {
    apiUrl: string;
    apiKeyLength: number;
    expectedApiKeyLength: number;
};

type HostResponse = {
    status: number;
};

const selectMode = (path: string): HealthcheckMode => {
    switch (path) {
        case "/healthcheck/thirdparty":
            return "healthcheck";

        case "/healthcheck/thirdparty/info":
            return "report";

        default:
            throw new CriError(400, `Unexpected resource path: ${path}`);
    }
};

const readPositiveNumberEnvironmentVariable = (name: string): number => {
    const rawValue = process.env[name];
    const value = Number(rawValue);

    if (!rawValue || !Number.isFinite(value) || value <= 0) {
        throw new CriError(500, `${name} must be a positive number`);
    }

    return value;
};

const readPositiveIntegerEnvironmentVariable = (name: string): number => {
    const value = readPositiveNumberEnvironmentVariable(name);

    if (!Number.isSafeInteger(value)) {
        throw new CriError(500, `${name} must be a positive integer`);
    }

    return value;
};

const loadEnvironmentVariables = (): EnvironmentVariables => {
    const stackName = process.env.AWS_STACK_NAME;
    const secretPrefix = process.env.SECRET_PREFIX;
    const clientId = process.env.CLIENT_ID;

    if (!stackName) {
        throw new CriError(500, "AWS_STACK_NAME environment variable is required");
    }

    if (!secretPrefix) {
        throw new CriError(500, "SECRET_PREFIX environment variable is required");
    }

    if (!clientId) {
        throw new CriError(500, "CLIENT_ID environment variable is required");
    }

    const timeoutSeconds = readPositiveNumberEnvironmentVariable("TIMEOUT_TIME_SECONDS");

    const expectedApiKeyLength = readPositiveIntegerEnvironmentVariable("EXPECTED_OS_API_KEY_LENGTH");

    return {
        stackName,
        secretPrefix,
        clientId,
        timeoutMs: timeoutSeconds * 1000,
        expectedApiKeyLength,
    };
};

const calculateRemainingTime = (startedAt: number, totalTimeoutMs: number): number => {
    const elapsedMs = performance.now() - startedAt;
    const remainingMs = Math.floor(totalTimeoutMs - elapsedMs);
    return Math.max(1, remainingMs);
};

const loadOsConfig = async (stackName: string, secretPrefix: string, clientId: string): Promise<HealthcheckConfig> => {
    const [osApiUrl, osApiKey] = await Promise.all([
        getParameterValue(stackName, `OrdnanceSurveyAPIUrl/${clientId}`),
        getSecretValue(secretPrefix, "OrdnanceSurveyAPIKey"),
    ]);

    return {
        osApiUrl,
        osApiKey,
    };
};

const validateOsConfig =
    (expectedApiKeyLength: number) =>
    (config: HealthcheckConfig | undefined): HealthcheckValidationResult => {
        if (!config?.osApiUrl) {
            return {
                success: false,
                message: "Ordnance Survey API URL is not configured",
            };
        }

        try {
            new URL(config.osApiUrl);
        } catch {
            return {
                success: false,
                message: "Ordnance Survey API URL is invalid",
            };
        }

        if (!config.osApiKey) {
            return {
                success: false,
                message: "Ordnance Survey API key is not configured",
            };
        }

        if (config.osApiKey.length !== expectedApiKeyLength) {
            return {
                success: false,
                message: "Ordnance Survey API key has unexpected length",
            };
        }

        return {
            success: true,
        };
    };

const checkOsHost = async (osApiUrl: string, signal: AbortSignal): Promise<HostResponse> => {
    const osHost = new URL(osApiUrl).origin;

    try {
        const response = await fetch(osHost, {
            method: "HEAD",
            signal,
        });

        return {
            status: response.status,
        };
    } catch {
        throw new Error("Unable to contact the Ordnance Survey API host");
    }
};

const validateHostResponse = (result: HostResponse | undefined): HealthcheckValidationResult => {
    if (!result) {
        return {
            success: false,
            message: "Ordnance Survey API host returned no response",
        };
    }

    if (result.status >= 500) {
        return {
            success: false,
            message: "Ordnance Survey API host is unavailable",
        };
    }

    return {
        success: true,
    };
};

const lookupTestPostcode = async (
    osApiUrl: string,
    osApiKey: string,
    signal: AbortSignal,
): Promise<CanonicalAddress[]> => {
    const url = new URL(osApiUrl);

    url.searchParams.set("postcode", TEST_POSTCODE);

    let response: Response;

    try {
        response = await fetch(url, {
            signal,
            headers: {
                Accept: "application/json",
                key: osApiKey,
            },
        });
    } catch {
        throw new Error("Unable to call the Ordnance Survey API");
    }

    if (!response.ok) {
        throw new Error(`Ordnance Survey API returned status ${response.status}`);
    }

    let responseBody: string;

    try {
        responseBody = await response.text();
    } catch {
        throw new Error("Unable to read the Ordnance Survey API response");
    }

    try {
        return processOrdnanceSurveySuccessResponse(responseBody);
    } catch {
        throw new Error("Unable to process the Ordnance Survey API response");
    }
};

const normalizePostcode = (postcode: string): string => postcode.replaceAll(" ", "").trim().toUpperCase();

const containsTenDowningStreet = (addresses: CanonicalAddress[] | undefined): boolean =>
    addresses?.some((address) => {
        const buildingNumber = address.buildingNumber?.trim().toUpperCase();

        const streetName = address.streetName?.trim().toUpperCase();

        const postalCode = address.postalCode;

        return (
            buildingNumber === TEST_BUILDING_NUMBER &&
            streetName === TEST_STREET_NAME &&
            postalCode !== undefined &&
            normalizePostcode(postalCode) === normalizePostcode(TEST_POSTCODE)
        );
    }) ?? false;

const validatePostcodeResponse = (addresses: CanonicalAddress[] | undefined): HealthcheckValidationResult => {
    if (!addresses) {
        return {
            success: false,
            message: "Ordnance Survey API returned no address response",
        };
    }

    if (!containsTenDowningStreet(addresses)) {
        return {
            success: false,
            message: "10 Downing Street was not returned by the Ordnance Survey API",
        };
    }

    return {
        success: true,
    };
};

const passed = (result: AssertFunctionResultOutput<unknown> | undefined): boolean =>
    result !== undefined && result.error === undefined;

class HealthcheckHandler implements LambdaInterface {
    @logger.injectLambdaContext({
        resetKeys: true,
    })
    @metrics.logMetrics({
        throwOnEmptyMetrics: false,
        captureColdStartMetric: true,
    })
    public async handler({ path }: APIGatewayProxyEvent, _context: Context): Promise<APIGatewayProxyResult> {
        try {
            const mode = selectMode(path);

            const { stackName, secretPrefix, clientId, timeoutMs, expectedApiKeyLength } = loadEnvironmentVariables();

            const healthcheckStartedAt = performance.now();

            logger.info(`mode=${mode}. Fetching OS configuration.`);

            const configResult = await assertFunctionResultWithTimeout(
                mode,
                calculateRemainingTime(healthcheckStartedAt, timeoutMs),
                () => loadOsConfig(stackName, secretPrefix, clientId),
                validateOsConfig(expectedApiKeyLength),
            );

            const config = configResult.result;

            let hostResult: AssertFunctionResultOutput<HostResponse> | undefined;

            let postcodeResult: AssertFunctionResultOutput<CanonicalAddress[]> | undefined;

            if (config && passed(configResult)) {
                logger.info("Checking Ordnance Survey API host connectivity");

                hostResult = await assertFunctionResultWithTimeout(
                    mode,
                    calculateRemainingTime(healthcheckStartedAt, timeoutMs),
                    (signal) => checkOsHost(config.osApiUrl, signal),
                    validateHostResponse,
                );

                if (passed(hostResult)) {
                    logger.info("Calling Ordnance Survey API with healthcheck postcode");

                    postcodeResult = await assertFunctionResultWithTimeout(
                        mode,
                        calculateRemainingTime(healthcheckStartedAt, timeoutMs),
                        (signal) => lookupTestPostcode(config.osApiUrl, config.osApiKey, signal),
                        validatePostcodeResponse,
                    );
                }
            }

            const healthcheckPassed = passed(configResult) && passed(hostResult) && passed(postcodeResult);

            if (mode === "healthcheck") {
                return {
                    statusCode: 200,
                    body: JSON.stringify({
                        message: "success",
                    }),
                };
            }

            const reportedConfig: ReportedConfig | undefined = config
                ? {
                      apiUrl: config.osApiUrl,
                      apiKeyLength: config.osApiKey.length,
                      expectedApiKeyLength,
                  }
                : undefined;

            return {
                statusCode: 200,
                body: JSON.stringify({
                    healthcheckPassed,

                    configuration: {
                        result: reportedConfig,
                        latency: configResult.latency,
                        error: configResult.error,
                    },

                    host: hostResult
                        ? {
                              result: hostResult.result,
                              latency: hostResult.latency,
                              error: hostResult.error,
                          }
                        : "N/A (not called due to earlier failures)",

                    postcodeLookup: postcodeResult
                        ? {
                              result: postcodeResult.result,
                              latency: postcodeResult.latency,
                              error: postcodeResult.error,
                          }
                        : "N/A (not called due to earlier failures)",
                }),
            };
        } catch (error) {
            return formatErrorResponse(error);
        }
    }
}

const handlerClass = new HealthcheckHandler();

export const handler = handlerClass.handler.bind(handlerClass);
