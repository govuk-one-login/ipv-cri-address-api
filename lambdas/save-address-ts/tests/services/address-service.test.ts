import { type DynamoDBDocument, PutCommand } from "@aws-sdk/lib-dynamodb";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
    AddressProcessingError,
    applyAddressValidity,
    isCurrentAddress,
    isInvalidAddress,
    isNotCurrentAddress,
    parseAddresses,
    saveAddresses,
    storeAddressEntryTypeMetrics,
} from "../../src/services/address-service.js";

const { mockCaptureMetric } = vi.hoisted(() => ({
    mockCaptureMetric: vi.fn(),
}));

vi.mock("@govuk-one-login/cri-metrics", () => ({
    captureMetric: mockCaptureMetric,
}));

const SESSION_ID = "00000000-0000-0000-0000-000000000001";

const ADDRESS_TTL = 1730197212;

const currentAddress = {
    uprn: 12345,
    addressCountry: "GB",
    postalCode: "SW1A 2AA",
    validFrom: "2020-01-01",
};

const previousAddress = {
    addressCountry: "GB",
    postalCode: "M1 1AE",
};

describe("address-service", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe("parseAddresses", () => {
        it("parses valid addresses", () => {
            const addresses = [currentAddress];

            expect(parseAddresses(JSON.stringify(addresses))).toEqual(addresses);
        });

        it("returns an empty array when no addresses are provided", () => {
            expect(parseAddresses("[]")).toEqual([]);
        });

        it("throws when the body is null", () => {
            expect(() => parseAddresses(null)).toThrow("could not parse addresses");
        });

        it("throws when the body contains invalid JSON", () => {
            expect(() => parseAddresses("not-json")).toThrow("could not parse addresses");
        });

        it("throws when the parsed body is not an array", () => {
            expect(() => parseAddresses(JSON.stringify(currentAddress))).toThrow("could not parse addresses");
        });

        it("does not treat whitespace as a missing country code", () => {
            const address = {
                ...currentAddress,
                addressCountry: " ",
            };

            expect(parseAddresses(JSON.stringify([address]))).toEqual([address]);
        });

        it.each([
            {
                description: "uprn is a string",
                address: {
                    uprn: "12345",
                    addressCountry: "GB",
                },
            },
            {
                description: "addressCountry is null",
                address: {
                    addressCountry: null,
                },
            },
            {
                description: "validFrom is a number",
                address: {
                    validFrom: 12345,
                    addressCountry: "GB",
                },
            },
            {
                description: "validUntil is a boolean",
                address: {
                    validUntil: true,
                    addressCountry: "GB",
                },
            },
            {
                description: "postalCode is a number",
                address: {
                    postalCode: 12345,
                    addressCountry: "GB",
                },
            },
            {
                description: "array element is a string",
                address: "not-an-address",
            },
            {
                description: "array element is null",
                address: null,
            },
        ])("throws when $description", ({ address }) => {
            expect(() => parseAddresses(JSON.stringify([address]))).toThrow("could not parse addresses");
        });

        it.each([
            "2010-00-00",
            "2021-02-30",
            "2021-02-29",
            "2021-13-01",
            "2021-01-01T00:00:00Z",
            "2024-1-01",
            "0000-01-01",
            "not-a-date",
        ])("throws when validFrom is an invalid date: %s", (validFrom) => {
            expect(() =>
                parseAddresses(
                    JSON.stringify([
                        {
                            addressCountry: "GB",
                            validFrom,
                        },
                    ]),
                ),
            ).toThrow("could not parse addresses");
        });

        it("throws when validUntil contains an invalid date", () => {
            expect(() =>
                parseAddresses(
                    JSON.stringify([
                        {
                            addressCountry: "GB",
                            validFrom: "2020-01-01",
                            validUntil: "2021-00-00",
                        },
                    ]),
                ),
            ).toThrow("could not parse addresses");
        });

        it.each(["2021-01-01", "2024-02-29", "1234-12-12"])("accepts valid date: %s", (validFrom) => {
            const address = {
                addressCountry: "GB",
                validFrom,
            };

            expect(parseAddresses(JSON.stringify([address]))).toEqual([address]);
        });

        it.each([
            {
                description: "missing",
                addressCountry: undefined,
            },
            {
                description: "empty",
                addressCountry: "",
            },
        ])("throws when the country code is $description", ({ addressCountry }) => {
            const address = {
                uprn: 12345,
                postalCode: "SW1A 2AA",
                validFrom: "2020-01-01",
                ...(addressCountry !== undefined && {
                    addressCountry,
                }),
            };

            expect(() => parseAddresses(JSON.stringify([address]))).toThrow("Country code not present for address");
        });

        it("removes fields that are not part of CanonicalAddress", () => {
            const address = {
                uprn: 10022812929,
                buildingNumber: "8",
                streetName: "HADLEY ROAD",
                addressLocality: "BATH",
                postalCode: "ba2 5aa",
                addressCountry: "GB",
                validFrom: "2021-01-01",
                unexpectedField: "should-not-be-stored",
                nested: {
                    anything: ["goes", 123],
                },
            };

            expect(parseAddresses(JSON.stringify([address]))).toEqual([
                {
                    uprn: 10022812929,
                    buildingNumber: "8",
                    streetName: "HADLEY ROAD",
                    addressLocality: "BATH",
                    postalCode: "ba2 5aa",
                    addressCountry: "GB",
                    validFrom: "2021-01-01",
                },
            ]);
        });

        it("redacts PII when an address fails validation", () => {
            const address = {
                uprn: "72262801",
                buildingNumber: "8",
                streetName: "GRANGE FIELDS WAY",
                addressLocality: "LEEDS",
                postalCode: "LS10 4QL",
                addressCountry: "GB",
                addressRegion: "YORKSHIRE",
                validFrom: "2010-00-00",
                validUntil: "2021-01-16",
            };

            expect(() => parseAddresses(JSON.stringify([address]))).toThrow(
                "Error while deserializing object. Some PII fields were redacted.",
            );
        });

        it("does not expose address PII when validation fails", () => {
            const address = {
                streetName: "GRANGE FIELDS WAY",
                postalCode: "LS10 4QL",
                addressCountry: "GB",
                validFrom: "2010-00-00",
            };

            try {
                parseAddresses(JSON.stringify([address]));

                expect.unreachable("Expected address parsing to fail");
            } catch (error) {
                expect(error).toBeInstanceOf(AddressProcessingError);

                const message = (error as Error).message;

                expect(message).toContain("Some PII fields were redacted");

                expect(message).not.toContain("GRANGE FIELDS WAY");

                expect(message).not.toContain("LS10 4QL");

                expect(message).not.toContain("2010-00-00");
            }
        });
    });

    describe("address validity helpers", () => {
        it("identifies a current address", () => {
            expect(isCurrentAddress(currentAddress)).toBe(true);
        });

        it("identifies a non-current address", () => {
            expect(isNotCurrentAddress(previousAddress)).toBe(true);
        });

        it("identifies an address as invalid when validFrom equals validUntil", () => {
            expect(
                isInvalidAddress({
                    ...currentAddress,
                    validUntil: "2020-01-01",
                }),
            ).toBe(true);
        });
    });

    describe("applyAddressValidity", () => {
        it("accepts one current address", () => {
            expect(applyAddressValidity([currentAddress])).toEqual([currentAddress]);
        });

        it("rejects one non-current address", () => {
            expect(() => applyAddressValidity([previousAddress])).toThrow(
                "applyAddressValidity found a single address but is not a CURRENT address.",
            );
        });

        it("sets previous validUntil from current validFrom", () => {
            expect(applyAddressValidity([currentAddress, previousAddress])).toEqual([
                currentAddress,
                {
                    ...previousAddress,
                    validUntil: "2020-01-01",
                },
            ]);
        });

        it("links addresses regardless of their order", () => {
            expect(applyAddressValidity([previousAddress, currentAddress])).toEqual([
                {
                    ...previousAddress,
                    validUntil: "2020-01-01",
                },
                currentAddress,
            ]);
        });

        it("allows two current addresses without linking them", () => {
            const secondCurrentAddress = {
                ...currentAddress,
                uprn: 67890,
                postalCode: "M1 1AE",
                validFrom: "2021-01-01",
            };

            expect(applyAddressValidity([currentAddress, secondCurrentAddress])).toEqual([
                currentAddress,
                secondCurrentAddress,
            ]);
        });

        it("rejects two non-current addresses", () => {
            expect(() =>
                applyAddressValidity([
                    previousAddress,
                    {
                        ...previousAddress,
                        postalCode: "B1 1AA",
                    },
                ]),
            ).toThrow("applyAddressValidity found two addresses but could not determine which address is a CURRENT.");
        });

        it("rejects equal validFrom and validUntil", () => {
            const invalidAddress = {
                ...currentAddress,
                validUntil: "2020-01-01",
            };

            expect(() => applyAddressValidity([invalidAddress, previousAddress])).toThrow(
                "applyAddressValidity found address where validFrom and validUntil are Equal.",
            );
        });

        it("rejects a previous address when validUntil is already set", () => {
            const previousWithValidUntil = {
                ...previousAddress,
                validUntil: "2019-01-01",
            };

            expect(() => applyAddressValidity([currentAddress, previousWithValidUntil])).toThrow(
                "applyAddressValidity found PREVIOUS address with a validUntil  already set - automatic date linking failed.",
            );
        });

        it("rejects more than two addresses", () => {
            expect(() =>
                applyAddressValidity([
                    currentAddress,
                    previousAddress,
                    {
                        ...previousAddress,
                        postalCode: "B1 1AA",
                    },
                ]),
            ).toThrow("applyAddressValidity received too many Addresses to process.");
        });

        it("does not mutate the input addresses", () => {
            const addresses = [currentAddress, previousAddress];

            const original = structuredClone(addresses);

            applyAddressValidity(addresses);

            expect(addresses).toEqual(original);
        });
    });

    describe("saveAddresses", () => {
        const send = vi.fn();

        const dynamoDbClient = {
            send,
        } as unknown as DynamoDBDocument;

        beforeEach(() => {
            send.mockResolvedValue({});
        });

        it("saves the addresses, session ID and TTL", async () => {
            await saveAddresses(SESSION_ID, [currentAddress], ADDRESS_TTL, {
                dynamoDbClient,
                addressTableName: "test-address-table",
            });

            expect(send).toHaveBeenCalledOnce();

            const command = send.mock.calls[0][0];

            expect(command).toBeInstanceOf(PutCommand);

            expect(command.input).toEqual({
                TableName: "test-address-table",
                Item: {
                    sessionId: SESSION_ID,
                    expiryDate: ADDRESS_TTL,
                    addresses: [
                        {
                            ...currentAddress,
                            postalCode: "SW1A2AA",
                        },
                    ],
                },
            });
        });

        it("normalises the postcode for GB addresses", async () => {
            await saveAddresses(
                SESSION_ID,
                [
                    {
                        ...currentAddress,
                        postalCode: "sw1a 2aa",
                    },
                ],
                ADDRESS_TTL,
                {
                    dynamoDbClient,
                    addressTableName: "test-address-table",
                },
            );

            const command = send.mock.calls[0][0];

            expect(command.input.Item.addresses[0].postalCode).toBe("SW1A2AA");
        });

        it.each([
            {
                countryCode: "gb",
                expectedPostcode: "SW1A2AA",
            },
            {
                countryCode: "UK",
                expectedPostcode: "sw1a 2aa",
            },
            {
                countryCode: "GBR",
                expectedPostcode: "sw1a 2aa",
            },
        ])(
            "handles postcode normalisation for country code $countryCode",
            async ({ countryCode, expectedPostcode }) => {
                await saveAddresses(
                    SESSION_ID,
                    [
                        {
                            ...currentAddress,
                            addressCountry: countryCode,
                            postalCode: "sw1a 2aa",
                        },
                    ],
                    ADDRESS_TTL,
                    {
                        dynamoDbClient,
                        addressTableName: "test-address-table",
                    },
                );

                const command = send.mock.calls[0][0];

                expect(command.input.Item.addresses[0].postalCode).toBe(expectedPostcode);
            },
        );

        it("does not mutate the submitted address during postcode normalisation", async () => {
            const address = {
                ...currentAddress,
                postalCode: "sw1a 2aa",
            };

            await saveAddresses(SESSION_ID, [address], ADDRESS_TTL, {
                dynamoDbClient,
                addressTableName: "test-address-table",
            });

            expect(address.postalCode).toBe("sw1a 2aa");
        });
    });

    describe("storeAddressEntryTypeMetrics", () => {
        it("records pre-populated address metric when UPRN is present", () => {
            storeAddressEntryTypeMetrics([currentAddress]);

            expect(mockCaptureMetric).toHaveBeenCalledWith("pre-populated-address-entry");
        });

        it("records manual address metric when UPRN is absent", () => {
            storeAddressEntryTypeMetrics([previousAddress]);

            expect(mockCaptureMetric).toHaveBeenCalledWith("manual-address-entry");
        });

        it("records one entry metric for each address", () => {
            storeAddressEntryTypeMetrics([currentAddress, previousAddress]);

            expect(mockCaptureMetric).toHaveBeenNthCalledWith(1, "pre-populated-address-entry");

            expect(mockCaptureMetric).toHaveBeenNthCalledWith(2, "manual-address-entry");

            expect(mockCaptureMetric).toHaveBeenCalledTimes(2);
        });
    });
});
