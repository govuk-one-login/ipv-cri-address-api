import { ApiError } from "./error-handler";
import { cleanMetricValue } from "./metric-value";

export const POSTCODE_LOOKUP_ERROR = "postcode_lookup_error";

export const POSTCODE_LOOKUP_ERROR_TYPE = "postcode_lookup_error_type";

export const POSTCODE_LOOKUP_ERROR_MESSAGE = "postcode_lookup_error_message";

export const getPostcodeErrorType = (error: unknown): string => {
    if (!(error instanceof ApiError)) {
        return "lookup_server";
    }

    switch (error.statusCode) {
        case 400:
            return error.message.includes("Client ID") ? "lookup_server" : "invalid_postcode_param";

        case 403:
            return error.message.toLowerCase().includes("expired") ? "session_expired" : "session_not_found";

        case 408:
            return "time_out_error";

        case 404:
            return "lookup_processing";

        default:
            return "lookup_server";
    }
};

export const getPostcodeErrorDimensions = (error: unknown): Record<string, string> => {
    const message = error instanceof Error ? error.message : "Unknown error";

    return {
        [POSTCODE_LOOKUP_ERROR_TYPE]: getPostcodeErrorType(error),
        [POSTCODE_LOOKUP_ERROR_MESSAGE]: cleanMetricValue(message),
    };
};
