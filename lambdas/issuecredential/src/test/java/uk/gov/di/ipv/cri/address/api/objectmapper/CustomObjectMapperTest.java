package uk.gov.di.ipv.cri.address.api.objectmapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomObjectMapperTest {

    @Test
    void shouldConfigureObjectMapperCorrectly() {
        ObjectMapper objectMapper = CustomObjectMapper.getMapperWithCustomSerializers();

        assertEquals(
                JsonInclude.Include.NON_NULL,
                objectMapper
                        .getSerializationConfig()
                        .getDefaultPropertyInclusion()
                        .getValueInclusion());
        assertEquals(3, objectMapper.getRegisteredModuleIds().size());
        assertFalse(
                objectMapper
                        .getSerializationConfig()
                        .isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    @Test
    void shouldSerializeJWTClaimsSetCorrectly() throws IOException {
        ObjectMapper objectMapper = CustomObjectMapper.getMapperWithCustomSerializers();

        JWTClaimsSet claimsSet =
                new JWTClaimsSet.Builder()
                        .subject("subject")
                        .issuer("issuer")
                        .expirationTime(new Date(4070909400000L))
                        .build();

        String expectedJson = "{\"iss\":\"issuer\",\"sub\":\"subject\",\"exp\":4070909400}";

        String actualJson = objectMapper.writeValueAsString(claimsSet);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    void shouldSerializeWithCredentialSubject() throws IOException {
        ObjectMapper objectMapper = CustomObjectMapper.getMapperWithCustomSerializers();

        CanonicalAddress address = new CanonicalAddress();
        address.setAddressCountry("GB");
        address.setBuildingName("");
        address.setStreetName("HADLEY ROAD");
        address.setPostalCode("BA2 5AA");
        address.setBuildingNumber("8");
        address.setAddressLocality("BATH");
        address.setPostalCode("BA2 5AA");

        address.setValidFrom(LocalDate.of(2000, 1, 1));
        Map<String, Object> vc = new HashMap<>();
        vc.put("credentialSubject", Map.of("address", List.of(address)));

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().claim("vc", vc).build();

        String actualJson = objectMapper.writeValueAsString(claimsSet);

        assertEquals(
                "{\"vc\":{\"credentialSubject\":{\"address\":[{\"addressCountry\":\"GB\",\"buildingName\":\"\",\"streetName\":\"HADLEY ROAD\",\"postalCode\":\"BA2 5AA\",\"buildingNumber\":\"8\",\"addressLocality\":\"BATH\",\"validFrom\":\"2000-01-01\"}]}}}",
                actualJson);
    }
}
