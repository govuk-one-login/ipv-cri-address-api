import { SSMClient } from "@aws-sdk/client-ssm";

export const SsmClient = new SSMClient({ region: "eu-west-2" });
