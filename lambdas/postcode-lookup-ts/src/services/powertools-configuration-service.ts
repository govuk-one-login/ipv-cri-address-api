import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { getSecret } from "@aws-lambda-powertools/parameters/secrets";

import type { ConfigurationService } from "./configuration-service";

export class PowertoolsConfigurationService implements ConfigurationService {
    constructor(
        private readonly stackName: string,
        private readonly secretPrefix: string,
    ) {}

    async getParameterValue(parameterName: string): Promise<string> {
        const fullParameterName = `/${this.stackName}/${parameterName}`;

        const value = await getParameter(fullParameterName);

        if (!value) {
            throw new Error(`Missing parameter ${fullParameterName}`);
        }

        return value;
    }

    async getSecretValue(secretName: string): Promise<string> {
        const fullSecretName = `/${this.secretPrefix}/${secretName}`;

        const value = await getSecret(fullSecretName);

        if (!value) {
            throw new Error(`Missing secret ${fullSecretName}`);
        }

        return value;
    }
}
