const MAX_METRIC_VALUE_LENGTH = 250;
const NO_CONTENT = "no_content";

export const cleanMetricValue = (value: string | null | undefined): string => {
    if (value == null || value.trim().length === 0) {
        return NO_CONTENT;
    }

    const stripped = value.replace(/^:+/, "");

    if (stripped.length === 0 || stripped.trim().length === 0) {
        return NO_CONTENT;
    }

    let cleaned = "";

    for (let index = 0; index < stripped.length; index += 1) {
        const character = stripped[index];
        const characterCode = stripped.charCodeAt(index);

        if (/\s/.test(character)) {
            cleaned += "_";
        } else if (characterCode >= 32 && characterCode < 127) {
            cleaned += character;
        } else {
            cleaned += "_";
        }
    }

    return cleaned.slice(0, MAX_METRIC_VALUE_LENGTH);
};
