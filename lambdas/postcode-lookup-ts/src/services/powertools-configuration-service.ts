import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { getSecret } from "@aws-lambda-powertools/parameters/secrets";

import type { ConfigurationService } from "./configuration-service";

export class PowertoolsConfigurationService implements ConfigurationService {
    async getParameterValue(parameterName: string): Promise<string> {
        const value = await getParameter(parameterName);

        if (!value) {
            throw new Error(`Missing parameter ${parameterName}`);
        }

        return value;
    }

    async getSecretValue(secretName: string): Promise<string> {
        const value = await getSecret(secretName);

        if (!value) {
            throw new Error(`Missing secret ${secretName}`);
        }

        return value;
    }
}
