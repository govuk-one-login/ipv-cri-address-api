package uk.gov.di.ipv.cri.address.api.objectmapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.nimbusds.jose.JWSHeader;

import java.io.IOException;

public class JWSHeaderSerializer extends JsonSerializer<JWSHeader> {
    @Override
    public void serialize(JWSHeader header, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();

        if (header.getType() != null) {
            gen.writeStringField("typ", header.getType().toString());
        } else {
            gen.writeStringField("typ", "JWT");
        }
        if (header.getAlgorithm() != null) {
            gen.writeStringField("alg", header.getAlgorithm().getName());
        }
        if (header.getKeyID() != null) {
            gen.writeStringField("kid", header.getKeyID());
        }

        gen.writeEndObject();
    }
}
