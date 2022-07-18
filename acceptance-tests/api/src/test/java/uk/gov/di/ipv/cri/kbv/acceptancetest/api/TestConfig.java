package uk.gov.di.ipv.cri.kbv.acceptancetest.api;

import com.nimbusds.oauth2.sdk.util.StringUtils;

import java.net.URI;
import java.util.Optional;

import static java.lang.String.format;

public class TestConfig {

    public static final String CLIENT_ID = getConfigValue("CLIENT_ID", "ipv-core-stub");

    public static final String PRIVATE_API_ID = getConfigValue("PRIVATE_API_ID", null);

    public static final String API_STAGE = getConfigValue("API_STAGE", "dev");

    public static final String PRIVATE_API_BASE_URL =
            "https://" + PRIVATE_API_ID + ".execute-api.eu-west-2.amazonaws.com/" + API_STAGE;

    public static final URI REDIRECT_URL =
            URI.create(
                    getConfigValue(
                            "REDIRECT_URL",
                            "https://di-ipv-core-stub.london.cloudapps.digital/callback"));

    public static final String AUDIENCE =
            getConfigValue("AUDIENCE", "https://review-a.dev.account.gov.uk");
    public static final String ISSUER =
            getConfigValue("ISSUER", "https://di-ipv-core-stub.london.cloudapps.digital");
    public static final String ENCRYPTION_PUBLIC_KEY_JWK_BASE64 =
            getConfigValue("ENCRYPTION_PUBLIC_KEY_JWK_BASE64", null);
    public static final String SIGNING_PRIVATE_KEY_JWK_BASE64 =
            getConfigValue("SIGNING_PRIVATE_KEY_JWK_BASE64", null);

    public static String getConfigValue(String key, String defaultValue) {
        String envValue = Optional.ofNullable(System.getenv(key)).orElse(defaultValue);
        if (StringUtils.isBlank(envValue)) {
            throw new IllegalStateException(
                    format("env var '%s' is not set and there is no default value", key));
        }
        return envValue;
    }

}
