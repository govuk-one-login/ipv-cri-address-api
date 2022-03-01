package uk.gov.di.ipv.cri.address.library.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;

import java.util.Objects;
import java.util.Optional;

public class ConfigurationService {

    private static final long DEFAULT_SESSION_ADDRESS_TTL_IN_SECS = 172800L;
    private final SSMProvider ssmProvider;
    private final String parameterPrefix;

    public ConfigurationService(SSMProvider ssmProvider, String parameterPrefix) {
        this.ssmProvider = ssmProvider;
        this.parameterPrefix = parameterPrefix;
    }

    public ConfigurationService() {
        this.ssmProvider = ParamManager.getSsmProvider();
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

    private String getParameterName(SSMParameterName parameterName) {
        return String.format("/%s/%s", parameterPrefix, parameterName.parameterName);
    }

    public enum SSMParameterName {
        ADDRESS_SESSION_TABLE_NAME("AddressSessionTableName"),
        ADDRESS_SESSION_TTL("AddressSessionTtl"),
        OS_API_KEY("OrdinanceSurveyAPIKey"),
        OS_API_URL("OrdinanceSurveyAPIUrl");

        public final String parameterName;

        SSMParameterName(String parameterName) {
            this.parameterName = parameterName;
        }
    }

    public String getOsApiKey() {
        return ssmProvider.get(getParameterName(SSMParameterName.OS_API_KEY));
    }

    public String getOsApiUrl() {
        return Optional.ofNullable(ssmProvider.get(getParameterName(SSMParameterName.OS_API_URL)))
                .orElse("https://api.os.uk/search/places/v1/postcode");
    }
}
