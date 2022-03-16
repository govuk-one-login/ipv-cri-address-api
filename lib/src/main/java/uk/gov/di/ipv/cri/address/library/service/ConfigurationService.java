package uk.gov.di.ipv.cri.address.library.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ConfigurationService {

    private static final long DEFAULT_SESSION_ADDRESS_TTL_IN_SECS = 172800L;
    private static final long DEFAULT_BEARER_TOKEN_TTL_IN_SECS = 3600L;
    private final SSMProvider ssmProvider;
    private final SecretsProvider secretsProvider;
    private final String parameterPrefix;

    public ConfigurationService(
            SSMProvider ssmProvider, SecretsProvider secretsProvider, String parameterPrefix) {
        this.ssmProvider = ssmProvider;
        this.secretsProvider = secretsProvider;
        this.parameterPrefix = parameterPrefix;
    }

    public ConfigurationService() {
        this.ssmProvider = ParamManager.getSsmProvider();
        this.secretsProvider = ParamManager.getSecretsProvider();
        this.parameterPrefix =
                Objects.requireNonNull(
                        System.getenv("AWS_STACK_NAME"), "env var AWS_STACK_NAME required");
    }

    public String getAddressSessionTableName() {
        return ssmProvider.get(getParameterName(SSMParameterName.ADDRESS_SESSION_TABLE_NAME));
    }

    public long getAddressSessionTtl() {
        return Optional.ofNullable(
                        ssmProvider.get(getParameterName(SSMParameterName.ADDRESS_SESSION_TTL)))
                .map(Long::valueOf)
                .orElse(DEFAULT_SESSION_ADDRESS_TTL_IN_SECS);
    }

    public String getParameterName(SSMParameterName parameterName) {
        return String.format("/%s/%s", parameterPrefix, parameterName.parameterName);
    }

    public Map<String, String> getParametersForPath(String path) {
        String format = String.format("/%s/%s", parameterPrefix, path);
        return ssmProvider.recursive().getMultiple(format.replace("//", "/"));
    }

    public URI getDynamoDbEndpointOverride() {
        String dynamoDbEndpointOverride = System.getenv("DYNAMODB_ENDPOINT_OVERRIDE");
        if (dynamoDbEndpointOverride != null && !dynamoDbEndpointOverride.isEmpty()) {
            return URI.create(System.getenv("DYNAMODB_ENDPOINT_OVERRIDE"));
        }
        return null;
    }

    public String getAddressAuthCodesTableName() {
        return System.getenv("ADDRESS_AUTH_CODES_TABLE_NAME");
    }

    public long getBearerAccessTokenTtl() {
        return Optional.ofNullable(System.getenv("BEARER_TOKEN_TTL"))
                .map(Long::valueOf)
                .orElse(DEFAULT_BEARER_TOKEN_TTL_IN_SECS);
    }

    public String getAccessTokensTableName() {
        return System.getenv("ADDRESS_ACCESS_TOKENS_TABLE_NAME");
    }

    public enum SSMParameterName {
        ADDRESS_SESSION_TABLE_NAME("AddressSessionTableName"),
        ADDRESS_SESSION_TTL("AddressSessionTtl"),
        ORDNANCE_SURVEY_API_KEY("OrdnanceSurveyAPIKey"),
        ORDNANCE_SURVEY_API_URL("OrdnanceSurveyAPIURL");

        public final String parameterName;

        SSMParameterName(String parameterName) {
            this.parameterName = parameterName;
        }
    }

    public String getOsApiKey() {
        return secretsProvider.get(getParameterName(SSMParameterName.ORDNANCE_SURVEY_API_KEY));
    }

    public String getOsPostcodeAPIUrl() {
        return ssmProvider.get(getParameterName(SSMParameterName.ORDNANCE_SURVEY_API_URL));
    }
}
