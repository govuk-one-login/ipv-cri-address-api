const POSTCODE_PATTERN = /\b[0-9A-Z]{1,4}\d[A-Z\d]?\s?\d[A-Z]{2}\b/gi;

const SO1_POSTCODE_PATTERN = /(\s{0,10}SO1\.\s{0,10}Requested\s+postcode\s+was\s{0,10})([A-Z0-9]+(?:\s+[A-Z0-9]+)?)/gi;

export const sanitize = (postcodePhrase: string | null | undefined): string | null | undefined => {
    if (postcodePhrase == null || postcodePhrase.length === 0) {
        return postcodePhrase;
    }

    const sanitizedSo1Response = postcodePhrase.replace(
        SO1_POSTCODE_PATTERN,
        (_match, prefix: string, postcode: string) => prefix + "*".repeat(postcode.length),
    );

    return sanitizedSo1Response.replace(POSTCODE_PATTERN, (postcode) => "*".repeat(postcode.length));
};
