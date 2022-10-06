export default {
    addressLookupStorageTableName: process.env.PERSON_IDENTITY_TABLE_NAME ?? "PersonIdentityItem",
    localDynamoUrl: process.env.LOCAL_DYNAMO_URL // Used to supply correct DynamoDb host name when running locally
        ? process.env.LOCAL_DYNAMO_URL // use value from /local-dev/local-env.json
        : "http://dynamodb:8000", // address required to run in integration test
};
