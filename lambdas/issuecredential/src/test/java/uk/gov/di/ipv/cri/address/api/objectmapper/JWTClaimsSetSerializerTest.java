package uk.gov.di.ipv.cri.address.api.objectmapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class JWTClaimsSetSerializerTest {
    private JWTClaimsSetSerializer serializer;
    private SerializerProvider serializerProvider;

    @BeforeEach
    void setUp() {
        serializer = new JWTClaimsSetSerializer();
        serializerProvider = mock(SerializerProvider.class);
    }

    @Test
    void shouldOrderClaimsAsExpectedWhenSerialized() throws IOException {
        JWTClaimsSet claimsSet =
                new JWTClaimsSet.Builder()
                        .notBeforeTime(new Date(4070908800000L))
                        .subject("subject")
                        .expirationTime(new Date(4070909400000L))
                        .jwtID("dummyJti")
                        .issuer("dummyAddressComponentId")
                        .claim("vc", Collections.emptyMap())
                        .build();

        StringWriter writer = new StringWriter();
        JsonGenerator generator = new ObjectMapper().getFactory().createGenerator(writer);

        serializer.serialize(claimsSet, generator, serializerProvider);
        generator.flush();

        String expectedJson =
                "{\"iss\":\"dummyAddressComponentId\",\"sub\":\"subject\",\"nbf\":4070908800,\"exp\":4070909400,\"vc\":{},\"jti\":\"dummyJti\"}";

        assertEquals(expectedJson, writer.toString().trim());
    }

    @Test
    void shouldSerializeEmptyClaimsSet() throws IOException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().build();

        StringWriter writer = new StringWriter();
        JsonGenerator generator = new ObjectMapper().getFactory().createGenerator(writer);

        serializer.serialize(claimsSet, generator, serializerProvider);
        generator.flush();

        String expectedJson = "{}";
        assertEquals(expectedJson, writer.toString());
    }
}
