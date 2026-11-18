const DEFAULT_REDACTION_LENGTH = 6;

const redactValue = (value: unknown): string => {
    let stringValue: string;

    if (value === null) {
        stringValue = "null";
    } else if (typeof value === "string") {
        stringValue = value;
    } else if (typeof value === "number" || typeof value === "boolean" || typeof value === "bigint") {
        stringValue = value.toString();
    } else if (typeof value === "object") {
        stringValue = JSON.stringify(value);
    } else {
        stringValue = "";
    }

    const redactionLength = stringValue.length === 0 ? DEFAULT_REDACTION_LENGTH : stringValue.length;

    return "*".repeat(redactionLength);
};

export const redactObjectFields = (value: Record<string, unknown>): Record<string, string> =>
    Object.fromEntries(Object.entries(value).map(([field, fieldValue]) => [field, redactValue(fieldValue)]));
