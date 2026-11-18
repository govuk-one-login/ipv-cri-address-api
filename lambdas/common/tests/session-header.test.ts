import { describe, expect, it } from "vitest";

import { getSessionId } from "../src/lib/session-header.js";
import { CriError } from "@govuk-one-login/cri-error-response";

describe("getSessionId", () => {
    it("returns the session ID from the headers", () => {
        expect(
            getSessionId({
                session_id: "00000000-0000-0000-0000-000000000001",
            }),
        ).toBe("00000000-0000-0000-0000-000000000001");
    });

    it("throws when the session ID is missing", () => {
        expect(() => getSessionId({})).toThrow("Session not found");
    });

    it("throws when the session ID is empty", () => {
        expect(() =>
            getSessionId({
                session_id: "",
            }),
        ).toThrow("Session not found");
    });

    it("throws when the session ID is whitespace", () => {
        expect(() =>
            getSessionId({
                session_id: "   ",
            }),
        ).toThrow("Session not found");
    });
    it("throws a 403 CriError when the session ID is missing", () => {
        try {
            getSessionId({});
            expect.fail("Expected getSessionId to throw");
        } catch (error) {
            expect(error).toBeInstanceOf(CriError);
            expect((error as CriError).statusCode).toBe(403);
            expect((error as CriError).message).toBe("Session not found");
        }
    });
});
