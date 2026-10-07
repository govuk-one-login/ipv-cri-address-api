import { Logger } from "@aws-lambda-powertools/logger";
import { describe, expect, it, vi } from "vitest";

import { PostcodeLookupService } from "../../src/services/postcode-lookup-service";

describe("PostcodeLookupService", () => {
    const mockLogger = {
        info: vi.fn(),
    } as unknown as Logger;

    it("returns empty array for 404 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 404,
            text: vi.fn().mockResolvedValue(""),
        });

        const service = new PostcodeLookupService(mockLogger, mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toEqual([]);
    });

    it("returns empty array for 400 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 400,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    error: {
                        statuscode: 400,
                        message: "Bad request",
                    },
                }),
            ),
        });

        const service = new PostcodeLookupService(mockLogger, mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toEqual([]);
    });

    it("returns mapped addresses for 200 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 200,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    results: [
                        {
                            DPA: {
                                UPRN: "123",
                                POSTCODE: "SW1A 2AA",
                                POST_TOWN: "London",
                            },
                        },
                    ],
                }),
            ),
        });

        const service = new PostcodeLookupService(mockLogger, mockFetch as typeof fetch);

        const result = await service.lookupPostcode("SW1A 2AA", "test-client");

        expect(result).toHaveLength(1);
        expect(result[0]).toMatchObject({
            uprn: "123",
            postalCode: "SW1A 2AA",
            addressLocality: "London",
            addressCountry: "GB",
        });
    });

    it("throws for 500 responses", async () => {
        const mockFetch = vi.fn().mockResolvedValue({
            status: 500,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({
                    error: {
                        statuscode: 500,
                        message: "Unexpected error",
                    },
                }),
            ),
        });

        const service = new PostcodeLookupService(mockLogger, mockFetch as typeof fetch);

        await expect(service.lookupPostcode("SW1A 2AA", "test-client")).rejects.toThrow();
    });

    it("throws when postcode is empty", async () => {
        const service = new PostcodeLookupService(mockLogger, vi.fn() as typeof fetch);

        await expect(service.lookupPostcode("", "test-client")).rejects.toThrow();
    });

    it("throws when postcode is whitespace", async () => {
        const service = new PostcodeLookupService(mockLogger, vi.fn() as typeof fetch);

        await expect(service.lookupPostcode("   ", "test-client")).rejects.toThrow();
    });
});
