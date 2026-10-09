export interface ConfigurationService {
    getParameterValue(parameterName: string): Promise<string>;

    getSecretValue(secretName: string): Promise<string>;
}
