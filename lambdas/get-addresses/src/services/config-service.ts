import { SSMClient, GetParameterCommand } from "@aws-sdk/client-ssm";
import { Config } from "../types/config";

export class ConfigService {
    constructor(private ssmClient: SSMClient) {}

    private parameterPrefix = process.env.AWS_STACK_NAME || "";
    readonly config: Config = {
        AddressLookupTableName: undefined,
    };

    public init(): Promise<void[]> {
        const keys = Object.keys(this.config);

        const promises = [];
        for (const key of keys) {
            promises.push(this.getParameter(key));
        }

        return Promise.all(promises);
    }

    private async getParameter(key: string): Promise<void> {
        const paramName = `/${this.parameterPrefix}/${key}`;

        const command = new GetParameterCommand({
            Name: paramName,
        });

        const addressLookupStorageTableName = await this.ssmClient.send(command);
        const value = addressLookupStorageTableName?.Parameter?.Value;
        const ObjKey = key as keyof Config;

        if (!value) {
            throw new Error(`Missing Parameter - ${key}`);
        } else {
            this.config[ObjKey] = value;
        }
    }
}
