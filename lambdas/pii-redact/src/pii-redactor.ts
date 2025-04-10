const personalNumberPatterns = [
    {
        regex: /\\"personalNumber\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"personalNumber\\": \\"***\\"',
    },
    {
        regex: /\\\\"personalNumber\\\\":\\\\"([^"]*)\\\\"/g,
        replacement: '\\\\"personalNumber\\\\":\\\\"***\\\\"',
    },
    {
        regex: /\\\\\\"personalNumber\\\\\\":\\\\\\"([^"]*)\\\\\\"/g,
        replacement: '\\\\\\"personalNumber\\\\\\":\\\\\\"***\\\\\\"',
    },
    {
        regex: /\\"nino\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"nino\\": \\"***\\"',
    },
    {
        regex: /\\\\"nino\\\\":\s*\\\\"([^"]*)\\\\"/g,
        replacement: '\\\\"nino\\\\": \\\\"***\\\\"',
    },
    {
        regex: /\\\\\\"nino\\\\\\":\\\\\\"([^"]*)\\\\\\"/g,
        replacement: '\\\\\\"nino\\\\\\": \\\\\\"***\\\\\\"',
    },
    {
        regex: /"nino":{"S": "\\"([^"]*)\\"}/g,
        replacement: '"nino":{"S": \\"***\\"}',
    },
    {
        regex: /\\"nino\\":{\\"S\\":"\\"([^"]*)\\"}/g,
        replacement: '\\"nino\\":{\\"S\\":\\"***\\"}',
    },
    {
        regex: /\\"nino\\":\{\\"S\\":\\"([^"]*)\\"}/g,
        replacement: '\\"nino\\":{\\"S\\":\\"***\\"}',
    },
];

const ipAddressPatterns = [
    {
        regex: /((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)/g,
        replacement: "***",
    },
    {
        regex: /\\"X-Forwarded-For\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"X-Forwarded-For\\": \\"***\\"',
    },
    {
        regex: /\\"clientIpAddress\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"clientIpAddress\\": \\"***\\"',
    },
    {
        regex: /\\"clientIpAddress\\":{\\"S\\":"\\"([^"]*)\\"}/g,
        replacement: '\\"nino\\":{\\"S\\":\\"***\\"}',
    },
    {
        regex: /\\"clientIpAddress\\":\{\\"S\\":\\"([^"]*)\\"}/g,
        replacement: '\\"clientIpAddress\\":{\\"S\\":\\"***\\"}',
    },
    {
        regex: /\\"ip_address\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"ip_address\\": \\"***\\"',
    },
    {
        regex: /\\\\"ip_address\\\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\\\\\"ip_address\\\\\\": \\"***\\"',
    },
    {
        regex: /"ip":\s*"([^"]*)"/g,
        replacement: '"ip": "***"',
    },
];

const namePatterns = [
    {
        regex: /\\"firstName\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"firstName\\": \\"***\\"',
    },
    {
        regex: /\\"lastName\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"lastName\\": \\"***\\"',
    },
    {
        regex: /\\"type\\":{\\"S\\":\\"GivenName\\"},\s*\\"value\\":{\\"S\\":\\"([^"]*)\\"/g,
        replacement: '\\"type\\": {\\"S\\": \\"GivenName\\"}, \\"value\\": {\\"S\\": \\"***\\"}',
    },
    {
        regex: /\\"type\\":{\\"S\\":\\"FamilyName\\"},\s*\\"value\\":{\\"S\\":\\"([^"]*)\\"/g,
        replacement: '\\"type\\": {\\"S\\": \\"FamilyName\\"}, \\"value\\": {\\"***\\"}',
    },
    {
        regex: /\\"type\\":\s*\\"FamilyName\\",\s*\\"value\\":\\"([^"]*)\\"/g,
        replacement: '\\"type\\": \\"FamilyName\\", \\"value\\": \\"***\\"',
    },
    {
        regex: /\\"type\\":\s*\\"GivenName\\",\s*\\"value\\":\\"([^"]*)\\"/g,
        replacement: '\\"type\\": \\"GivenName\\", \\"value\\": \\"***\\"',
    },
    {
        regex: /\\\\\\"GivenName\\\\\\",\\\\\\"value\\\\\\":\\\\\\"([^"]*)\\\\\\"/g,
        replacement: '\\\\\\"GivenName\\\\\\",\\\\\\"value\\\\\\":\\\\\\"***\\\\\\"',
    },
    {
        regex: /\\\\\\"FamilyName\\\\\\",\\\\\\"value\\\\\\":\\\\\\"([^"]*)\\\\\\"/g,
        replacement: '\\\\\\"FamilyName\\\\\\",\\\\\\"value\\\\\\":\\\\\\"***\\\\\\"',
    },
];

const dobPatterns = [
    {
        regex: /\\"dob\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"dob\\": \\"***\\"',
    },
    {
        regex: /\\"birthDates\\"\s*:\s*\{\s*\\"L\\"\s*:\s*\[\s*\{\s*\\"M\\"\s*:\s*\{\s*\\"value\\"\s*:\s*\{\s*\\"S\\"\s*:\s*\\"(\d{4}-\d{2}-\d{2})\\".*]\s*}/g,
        replacement: '\\"birthDates\\": \\"***\\"',
    },
    {
        regex: /\\"birthDate\\":\[{\\"value\\":\\"([^"]*)\\"}]/g,
        replacement: '\\"birthDate\\":[{\\"value\\":\\"***\\"}]',
    },
    {
        regex: /\\\\\\"birthDate\\\\\\":\[\{\\\\\\"value\\\\\\":\\\\\\"([^"]*)\\\\\\"}/g,
        replacement: '\\\\\\"birthDate\\\\\\":[{\\\\\\"value\\\\\\":\\\\\\"***\\\\\\"}]',
    },
    {
        regex: /\\"dateOfBirth\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"dateOfBirth\\": \\"***\\"',
    },
];

const addressPatterns = [
    {
        regex: /\\"buildingName\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"buildingName\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"addressLocality\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"addressLocality\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"buildingNumber\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"buildingNumber\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"postalCode\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"postalCode\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"streetName\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"streetName\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"uprn\\"\s*:\s*{\s*\\"N\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"uprn\\": { \\"N\\": \\"***\\"',
    },
    {
        regex: /\\"subBuildingName\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"subBuildingName\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"addressRegion\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"addressRegion\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"addressCountry\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"addressCountry\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"departmentName\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"departmentName\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"organisationName\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"organisationName\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"dependentAddressLocality\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"dependentAddressLocality\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"doubleDependentAddressLocality\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"doubleDependentAddressLocality\\": { \\"S\\": \\"***\\"',
    },
    {
        regex: /\\"dependentStreetName\\"\s*:\s*{\s*\\"S\\"\s*:\s*\\"([^"]*)\\"/g,
        replacement: '\\"dependentStreetName\\": { \\"S\\": \\"***\\"',
    },
];

const otherPatterns = [
    {
        regex: /\\"user_id\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"user_id\\": \\"***\\"',
    },
    {
        regex: /\\\\"user_id\\\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\\\\\"user_id\\\\\\": \\"***\\"',
    },
    {
        regex: /\\\\\\"user_id\\\\\\":\\\\\\"([^"]*)\\\\\\"/g,
        replacement: '\\\\\\"user_id\\\\\\": \\\\\\"***\\\\\\"',
    },
    {
        regex: /\\\\"subject\\\\":\{\\\\"S\\\\":\\\\"([^"]*)\\\\"/g,
        replacement: '\\\\\\"subject\\\\": \\\\\\"***\\\\\\"',
    },
    {
        regex: /\\"subject\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"subject\\": \\"***\\"',
    },
    {
        regex: /\\"subject\\":{\\"S\\": "\\"([^"]*)\\"}/g,
        replacement: '\\"subject\\":{\\"S\\": \\"***\\"}',
    },
    {
        regex: /\\"subject\\":\{\\"S\\":\\"([^"]*)\\"/g,
        replacement: '\\"subject\\":{\\"S\\":\\"***\\"',
    },
    {
        regex: /\\"token\\":\s*\\"([^"]*)\\"/g,
        replacement: '\\"token\\": \\"***\\"',
    },
];

export const redactPII = (message: string) => {
    const allPatterns = [
        ...personalNumberPatterns,
        ...ipAddressPatterns,
        ...namePatterns,
        ...dobPatterns,
        ...addressPatterns,
        ...otherPatterns,
    ];

    return allPatterns.reduce((redactedMessage, pattern) => {
        return redactedMessage.replaceAll(pattern.regex, pattern.replacement);
    }, message);
};
