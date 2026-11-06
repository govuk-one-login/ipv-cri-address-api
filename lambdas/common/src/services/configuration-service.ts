import { getParameter } from "@aws-lambda-powertools/parameters/ssm";
import { getSecret } from "@aws-lambda-powertools/parameters/secrets";

const DEFAULT_SESSION_TTL_IN_SECS = 7200;
const AUTHORIZATION_CODE_TTL_IN_SECS = 600;

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

    return typeof value === "string" ? value : new TextDecoder().decode(value);
};

export const getSessionExpirationEpoch = (currentTime: () => number = () => Math.floor(Date.now() / 1000)): number => {
    const ttl = process.env.SESSION_TTL ? Number.parseInt(process.env.SESSION_TTL, 10) : DEFAULT_SESSION_TTL_IN_SECS;

    return currentTime() + ttl;
};

export const getAuthorizationCodeExpirationEpoch = (
    currentTime: () => number = () => Math.floor(Date.now() / 1000),
): number => {
    const ttl = process.env.AUTHORIZATION_CODE_TTL
        ? Number.parseInt(process.env.AUTHORIZATION_CODE_TTL, 10)
        : AUTHORIZATION_CODE_TTL_IN_SECS;

    return currentTime() + ttl;
};
