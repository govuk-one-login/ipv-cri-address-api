package uk.gov.di.ipv.cri.address.api.objectmapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.nimbusds.jwt.JWTClaimsSet;

import java.io.IOException;
import java.util.Map;

public class JWTClaimsSetSerializer extends JsonSerializer<JWTClaimsSet> {

    public static final String BIRTH_DATE = "birthDate";
    public static final String NAME = "name";
    public static final String ADDRESS = "address";

    @Override
    public void serialize(JWTClaimsSet claimsSet, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();

        if (claimsSet.getIssuer() != null) {
            gen.writeStringField("iss", claimsSet.getIssuer());
        }
        if (claimsSet.getSubject() != null) {
            gen.writeStringField("sub", claimsSet.getSubject());
        }
        if (claimsSet.getNotBeforeTime() != null) {
            gen.writeNumberField("nbf", claimsSet.getNotBeforeTime().getTime() / 1000);
        }
        if (claimsSet.getExpirationTime() != null) {
            gen.writeNumberField("exp", claimsSet.getExpirationTime().getTime() / 1000);
        }

        serializeVcClaim(claimsSet.getClaim("vc"), gen);

        if (claimsSet.getJWTID() != null) {
            gen.writeStringField("jti", claimsSet.getJWTID());
        }

        gen.writeEndObject();
    }

    private void serializeVcClaim(Object vcClaim, JsonGenerator gen) throws IOException {
        if (vcClaim instanceof Map) {
            Map<String, Object> vc = (Map<String, Object>) vcClaim;
            gen.writeObjectFieldStart("vc");

            for (Map.Entry<String, Object> entry : vc.entrySet()) {
                if ("credentialSubject".equals(entry.getKey()) && entry.getValue() instanceof Map) {
                    serializeCredentialSubject((Map<String, Object>) entry.getValue(), gen);
                } else {
                    gen.writeObjectField(entry.getKey(), entry.getValue());
                }
            }

            gen.writeEndObject();
        }
    }

    private void serializeCredentialSubject(
            Map<String, Object> credentialSubject, JsonGenerator gen) throws IOException {
        gen.writeObjectFieldStart("credentialSubject");

        // Order the fields as name, birthDate, address
        if (credentialSubject.containsKey(NAME)) {
            gen.writeObjectField(NAME, credentialSubject.get(NAME));
        }
        if (credentialSubject.containsKey(BIRTH_DATE)) {
            gen.writeObjectField(BIRTH_DATE, credentialSubject.get(BIRTH_DATE));
        }
        if (credentialSubject.containsKey(ADDRESS)) {
            gen.writeObjectField(ADDRESS, credentialSubject.get(ADDRESS));
        }

        gen.writeEndObject();
    }
}
