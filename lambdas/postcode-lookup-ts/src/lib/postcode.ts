import type { PostcodeLookupRequest } from "../types/postcode-lookup.js";
import { PostcodeLookupError } from "../types/postcode-lookup-error.js";

const MISSING_POSTCODE_MESSAGE = "Missing postcode in request body.";
const INVALID_BODY_MESSAGE = "Failed to parse postcode from request body";
const BLANK_POSTCODE_MESSAGE = "Postcode must not be null or blank";

const parseRequestBody = (body: string | null): PostcodeLookupRequest => {
    if (body === null || body.trim() === "") {
        throw new PostcodeLookupError("BAD_REQUEST", MISSING_POSTCODE_MESSAGE);
    }

    try {
        const parsedBody: unknown = JSON.parse(body);

        if (typeof parsedBody !== "object" || parsedBody === null || Array.isArray(parsedBody)) {
            throw new PostcodeLookupError("BAD_REQUEST", MISSING_POSTCODE_MESSAGE);
        }

        return parsedBody as PostcodeLookupRequest;
    } catch (error) {
        if (error instanceof PostcodeLookupError) {
            throw error;
        }

        throw new PostcodeLookupError("BAD_REQUEST", INVALID_BODY_MESSAGE, { cause: error });
    }
};

export const validatePostcode = (postcode: string): string => {
    if (postcode.trim() === "") {
        throw new PostcodeLookupError("VALIDATION", BLANK_POSTCODE_MESSAGE);
    }

    return postcode;
};

export const getPostcodeFromRequest = (body: string | null): string => {
    const request = parseRequestBody(body);

    if (typeof request.value !== "string") {
        throw new PostcodeLookupError("BAD_REQUEST", MISSING_POSTCODE_MESSAGE);
    }

    return validatePostcode(request.value);
};

export const decodePostcode = (postcode: string): string => decodeURIComponent(postcode.replace(/\+/g, " "));
