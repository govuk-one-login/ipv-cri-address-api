import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { getSecret } from "@aws-lambda-powertools/parameters/secrets";

export const getParameterValue = async (stackName: string, parameterName: string): Promise<string> => {
    const fullParameterName = `/${stackName}/${parameterName}`;
    const value = await getParameter(fullParameterName);

    if (!value) {
        throw new Error(`Missing parameter ${fullParameterName}`);
    }

    return value;
};

export const getSecretValue = async (secretPrefix: string, secretName: string): Promise<string> => {
    const fullSecretName = `/${secretPrefix}/${secretName}`;
    const value = await getSecret(fullSecretName);

    if (!value) {
        throw new Error(`Missing secret ${fullSecretName}`);
    }

    return value;
};
