package uk.gov.di.ipv.cri.address.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock SSMProvider ssmProvider;
    @Mock SecretsProvider secretsProvider;
    private ConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configurationService = new ConfigurationService(ssmProvider, secretsProvider, "stack-name");
    }

    @Test
    void shouldGetAccessTokenTableName() {
        when(ssmProvider.get(
                        "/stack-name/"
                                + ConfigurationService.SSMParameterName.ADDRESS_SESSION_TABLE_NAME
                                        .parameterName))
                .thenReturn("the table name");
        assertEquals("the table name", configurationService.getAddressSessionTableName());
    }

    @Test
    void shouldGetBearerAccessTokenTtl() {
        when(ssmProvider.get(
                        "/stack-name/"
                                + ConfigurationService.SSMParameterName.ADDRESS_SESSION_TTL
                                        .parameterName))
                .thenReturn("10");
        assertEquals(10, configurationService.getAddressSessionTtl());
    }

    @Test
    void shouldGetOrdnanceSurveyAPIKey() {
        when(secretsProvider.get(
                        "/stack-name/"
                                + ConfigurationService.SSMParameterName.ORDNANCE_SURVEY_API_KEY
                                        .parameterName))
                .thenReturn("1234567890");

        assertEquals("1234567890", configurationService.getOsApiKey());
    }
}
