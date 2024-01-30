import { SSMClient } from "@aws-sdk/client-ssm";
import { NodeHttpHandler } from "@smithy/node-http-handler";
import { Agent } from "https";

const DEFAULT_CONNECTION_TIMEOUT = 30000;
const DEFAULT_SOCKET_TIMEOUT = 30000;

const requestHandler = new NodeHttpHandler({
    httpsAgent: new Agent(),
    connectionTimeout: DEFAULT_CONNECTION_TIMEOUT,
    socketTimeout: DEFAULT_SOCKET_TIMEOUT,
});

export const SsmClient = new SSMClient({
    region: "eu-west-2",
    requestHandler,
});
