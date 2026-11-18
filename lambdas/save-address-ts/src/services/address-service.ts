import { PutCommand, type DynamoDBDocument } from "@aws-sdk/lib-dynamodb";

import { CriError } from "@govuk-one-login/cri-error-response";
import { captureMetric } from "@govuk-one-login/cri-metrics";
import type { CanonicalAddress } from "../../../common/src/types/canonical-address.js";
import { redactObjectFields } from "../../../common/src/lib/pii-redactor.js";

const ERROR_COULD_NOT_PARSE_ADDRESSES = "could not parse addresses";

const ERROR_SINGLE_ADDRESS_NOT_CURRENT = "applyAddressValidity found a single address but is not a CURRENT address.";

const ERROR_TOO_MANY_ADDRESSES = "applyAddressValidity received too many Addresses to process.";

const ERROR_COULD_NOT_DETERMINE_CURRENT_ADDRESS =
    "applyAddressValidity found two addresses but could not determine which address is a CURRENT.";

const ERROR_ADDRESS_LINKING_NOT_NEEDED =
    "applyAddressValidity found PREVIOUS address with a validUntil  already set - automatic date linking failed.";

const ERROR_ADDRESS_DATE_IS_INVALID = "applyAddressValidity found address where validFrom and validUntil are Equal.";

const CANONICAL_ADDRESS_KEYS = [
    "validFrom",
    "validUntil",
    "uprn",
    "organisationName",
    "departmentName",
    "subBuildingName",
    "buildingName",
    "buildingNumber",
    "dependentStreetName",
    "streetName",
    "doubleDependentAddressLocality",
    "dependentAddressLocality",
    "addressLocality",
    "postalCode",
    "addressCountry",
    "addressRegion",
] as const satisfies readonly (keyof CanonicalAddress)[];

interface SaveAddressesDependencies {
    dynamoDbClient: DynamoDBDocument;
    addressTableName: string;
}

interface AddressItem {
    sessionId: string;
    expiryDate: number;
    addresses: CanonicalAddress[];
}

export const parseAddresses = (addressBody: string | null): CanonicalAddress[] => {
    if (addressBody === null) {
        throw new AddressProcessingError(ERROR_COULD_NOT_PARSE_ADDRESSES);
    }

    let parsedBody: unknown;

    try {
        parsedBody = JSON.parse(addressBody);
    } catch {
        throw new AddressProcessingError(ERROR_COULD_NOT_PARSE_ADDRESSES);
    }

    if (!Array.isArray(parsedBody)) {
        throw new AddressProcessingError(ERROR_COULD_NOT_PARSE_ADDRESSES);
    }

    const invalidAddress = parsedBody.find((address) => !isCanonicalAddress(address));

    if (invalidAddress !== undefined) {
        throw invalidAddressError(invalidAddress);
    }

    if (isCountryCodeAbsentForAny(parsedBody)) {
        throw new AddressProcessingError("Country code not present for address");
    }

    return parsedBody.map(pickCanonicalAddressFields);
};

const ISO_DATE_PATTERN = /^(?!0000)\d{4}-\d{2}-\d{2}$/;

const isOptionalString = (value: unknown): boolean => value === undefined || typeof value === "string";

const isOptionalNumber = (value: unknown): boolean => value === undefined || typeof value === "number";

const isOptionalDate = (value: unknown): boolean =>
    value === undefined || (typeof value === "string" && isIsoCalendarDate(value));

const isIsoCalendarDate = (value: string): boolean => {
    if (!ISO_DATE_PATTERN.test(value)) {
        return false;
    }

    const [year, month, day] = value.split("-").map(Number) as [number, number, number];

    const date = new Date(0);

    date.setUTCFullYear(year, month - 1, day);

    return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
};

const isCanonicalAddress = (value: unknown): value is CanonicalAddress => {
    if (typeof value !== "object" || value === null) {
        return false;
    }

    const address = value as Record<string, unknown>;

    return (
        isOptionalNumber(address.uprn) &&
        isOptionalDate(address.validFrom) &&
        isOptionalDate(address.validUntil) &&
        isOptionalString(address.organisationName) &&
        isOptionalString(address.departmentName) &&
        isOptionalString(address.subBuildingName) &&
        isOptionalString(address.buildingName) &&
        isOptionalString(address.buildingNumber) &&
        isOptionalString(address.dependentStreetName) &&
        isOptionalString(address.streetName) &&
        isOptionalString(address.doubleDependentAddressLocality) &&
        isOptionalString(address.dependentAddressLocality) &&
        isOptionalString(address.addressLocality) &&
        isOptionalString(address.postalCode) &&
        isOptionalString(address.addressCountry) &&
        isOptionalString(address.addressRegion)
    );
};

const pickCanonicalAddressFields = (address: CanonicalAddress): CanonicalAddress =>
    Object.fromEntries(
        CANONICAL_ADDRESS_KEYS.filter((key) => address[key] !== undefined).map((key) => [key, address[key]]),
    ) as CanonicalAddress;

const isCountryCodeAbsentForAny = (addresses: CanonicalAddress[]): boolean =>
    addresses.some((address) => address.addressCountry === undefined || address.addressCountry === "");

export const applyAddressValidity = (addresses: CanonicalAddress[]): CanonicalAddress[] => {
    const result = addresses.map((address) => ({
        ...address,
    }));

    switch (result.length) {
        case 1:
            if (!isCurrentAddress(result[0])) {
                throw new AddressProcessingError(ERROR_SINGLE_ADDRESS_NOT_CURRENT);
            }

            return result;

        case 2:
            return specifyValidUntilForPreviousAddress([result[0], result[1]]);

        default:
            throw new AddressProcessingError(ERROR_TOO_MANY_ADDRESSES);
    }
};

export const saveAddresses = async (
    sessionId: string,
    addresses: CanonicalAddress[],
    ttlExpiryEpoch: number,
    { dynamoDbClient, addressTableName }: SaveAddressesDependencies,
): Promise<void> => {
    const addressItem: AddressItem = {
        sessionId,
        expiryDate: ttlExpiryEpoch,
        addresses: addresses.map(normalizePostcodeForGbAddress),
    };

    await dynamoDbClient.send(
        new PutCommand({
            TableName: addressTableName,
            Item: addressItem,
        }),
    );
};

export const storeAddressEntryTypeMetrics = (addresses: CanonicalAddress[]): void => {
    for (const address of addresses) {
        const hasUprn = address.uprn !== undefined;

        captureMetric(hasUprn ? "pre-populated-address-entry" : "manual-address-entry");
    }
};

export const isCurrentAddress = (address: CanonicalAddress): boolean =>
    isPresent(address.validFrom) && !isPresent(address.validUntil);

export const isNotCurrentAddress = (address: CanonicalAddress): boolean => !isCurrentAddress(address);

export const isInvalidAddress = (address: CanonicalAddress): boolean =>
    isPresent(address.validFrom) && isPresent(address.validUntil) && address.validFrom === address.validUntil;

const specifyValidUntilForPreviousAddress = (
    addresses: [CanonicalAddress, CanonicalAddress],
): [CanonicalAddress, CanonicalAddress] => {
    const [firstAddress, secondAddress] = addresses;

    if (isInvalidAddress(firstAddress) || isInvalidAddress(secondAddress)) {
        throw new AddressProcessingError(ERROR_ADDRESS_DATE_IS_INVALID);
    }

    const firstIsCurrent = isCurrentAddress(firstAddress);

    const secondIsCurrent = isCurrentAddress(secondAddress);

    if (firstIsCurrent && secondIsCurrent) {
        return addresses;
    }

    let currentAddressIndex: number;
    let previousAddressIndex: number;

    if (firstIsCurrent && isNotCurrentAddress(secondAddress)) {
        currentAddressIndex = 0;
        previousAddressIndex = 1;
    } else if (secondIsCurrent && isNotCurrentAddress(firstAddress)) {
        currentAddressIndex = 1;
        previousAddressIndex = 0;
    } else {
        throw new AddressProcessingError(ERROR_COULD_NOT_DETERMINE_CURRENT_ADDRESS);
    }

    const currentAddress = addresses[currentAddressIndex];

    const previousAddress = addresses[previousAddressIndex];

    if (isPresent(previousAddress.validUntil)) {
        throw new AddressProcessingError(ERROR_ADDRESS_LINKING_NOT_NEEDED);
    }

    if (previousAddressIndex === 0) {
        return [
            {
                ...firstAddress,
                validUntil: currentAddress.validFrom,
            },
            secondAddress,
        ];
    }

    return [
        firstAddress,
        {
            ...secondAddress,
            validUntil: currentAddress.validFrom,
        },
    ];
};

const normalizePostcodeForGbAddress = (address: CanonicalAddress): CanonicalAddress => {
    if (!isGreatBritain(address.addressCountry) || !isPresent(address.postalCode)) {
        return {
            ...address,
        };
    }

    return {
        ...address,
        postalCode: address.postalCode.replaceAll(" ", "").toUpperCase(),
    };
};

const isGreatBritain = (addressCountry: CanonicalAddress["addressCountry"]): boolean =>
    addressCountry?.toUpperCase() === "GB";

const isPresent = <T>(value: T | null | undefined): value is T => value !== null && value !== undefined;

const invalidAddressError = (value: unknown): AddressProcessingError => {
    if (typeof value !== "object" || value === null || Array.isArray(value)) {
        return new AddressProcessingError(ERROR_COULD_NOT_PARSE_ADDRESSES);
    }

    return new AddressProcessingError(
        `${ERROR_COULD_NOT_PARSE_ADDRESSES}...` +
            "Error while deserializing object. " +
            "Some PII fields were redacted. " +
            JSON.stringify(redactObjectFields(value as Record<string, unknown>)),
    );
};

export class AddressProcessingError extends CriError {
    constructor(message: string) {
        super(500, message);
    }
}
