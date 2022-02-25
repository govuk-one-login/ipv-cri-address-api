package uk.gov.di.ipv.cri.address.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock SSMProvider ssmProvider;
    private ConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configurationService = new ConfigurationService(ssmProvider);
    }

    @Test
    void shouldGetAccessTokenTableName() {
        when(ssmProvider.get(
                        ConfigurationService.SSMParameterName.AddressSessionTableName.toString()))
                .thenReturn("the table name");
        assertEquals("the table name", configurationService.getAddressSessionTableName());
    }

    @Test
    void shouldGetBearerAccessTokenTtl() {
        when(ssmProvider.get(ConfigurationService.SSMParameterName.AddressSessionTtl.toString()))
                .thenReturn("10");
        assertEquals(10, configurationService.getAddressSessionTtl());
    }
}
