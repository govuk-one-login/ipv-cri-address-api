export type PostcodeLookupErrorCode = "BAD_REQUEST" | "VALIDATION" | "TIMEOUT" | "PROCESSING" | "UNSUPPORTED_CLIENT";

export class PostcodeLookupError extends Error {
    public constructor(
        public readonly code: PostcodeLookupErrorCode,
        message: string,
        options?: ErrorOptions,
    ) {
        super(message, options);
        this.name = "PostcodeLookupError";
    }
}
