package uk.gov.di.ipv.cri.address.api.handler;

import java.util.Optional;

public class EnvironmentService {
    public String getEnvironmentVariableOrThrow(String envVar) {
        return Optional.ofNullable(System.getenv(envVar)).orElseThrow();
    }
}
