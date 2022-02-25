package uk.gov.di.ipv.cri.address.library.service;

import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;

import java.util.Optional;

public class ConfigurationService {

    private static final long DEFAULT_SESSION_ADDRESS_TTL_IN_SECS = 172800L;
    private final SSMProvider ssmProvider;

    public ConfigurationService(SSMProvider ssmProvider) {
        this.ssmProvider = ssmProvider;
    }

    public ConfigurationService() {
        this.ssmProvider = ParamManager.getSsmProvider();
    }

    public String getAddressSessionTableName() {
        return ssmProvider.get(SSMParameterName.AddressSessionTableName.toString());
    }

    public long getAddressSessionTtl() {
        return Optional.ofNullable(ssmProvider.get(SSMParameterName.AddressSessionTtl.name()))
                .map(Long::valueOf)
                .orElse(DEFAULT_SESSION_ADDRESS_TTL_IN_SECS);
    }

    public enum SSMParameterName {
        AddressSessionTableName,
        AddressSessionTtl
    }
}
