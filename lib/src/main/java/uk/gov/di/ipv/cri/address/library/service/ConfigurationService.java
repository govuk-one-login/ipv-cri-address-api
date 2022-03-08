package uk.gov.di.ipv.cri.address.library.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;

import java.util.Objects;
import java.util.Optional;

import static uk.gov.di.ipv.cri.address.library.constants.OrdnanceSurveyConstants.POSTCODE_LOOKUP_API;

public class ConfigurationService {

    private static final long DEFAULT_SESSION_ADDRESS_TTL_IN_SECS = 172800L;
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

    public enum SSMParameterName {
        ADDRESS_SESSION_TABLE_NAME("AddressSessionTableName"),
        ADDRESS_SESSION_TTL("AddressSessionTtl"),
        OS_API_KEY("OrdinanceSurveyAPIKey");

        public final String parameterName;

        SSMParameterName(String parameterName) {
            this.parameterName = parameterName;
        }
    }

    public String getOsApiKey() {

        return secretsProvider.get(getParameterName(SSMParameterName.OS_API_KEY));
    }

    // This is exposed here so that we can unit test URL failures
    public String getOsPostcodeAPIUrl() {
        return POSTCODE_LOOKUP_API;
    }
}
