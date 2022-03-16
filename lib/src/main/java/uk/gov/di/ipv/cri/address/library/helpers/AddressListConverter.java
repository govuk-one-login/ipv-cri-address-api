package uk.gov.di.ipv.cri.address.library.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressListConverter implements AttributeConverter {
    ObjectMapper mapper;

    public AddressListConverter() {
        mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
    }

    @Override
    public AttributeValue transformFrom(Object input) {
        List<Object> addresses =
                input instanceof List ? (List<Object>) input : Arrays.asList(input);

        JavaType stringType = mapper.getTypeFactory().constructType(String.class);
        JavaType mapType =
                mapper.getTypeFactory().constructMapType(Map.class, stringType, stringType);

        String json = null;

        List<AttributeValue> list = new ArrayList<>();

        for (Object address : addresses) {
            Map<String, String> map = new HashMap<>();
            try {
                map = mapper.readValue(mapper.writeValueAsString(address), mapType);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            Map<String, AttributeValue> attributeValueMap = new HashMap<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                attributeValueMap.put(
                        entry.getKey(), AttributeValue.builder().s(entry.getValue()).build());
            }
            list.add(AttributeValue.builder().m(attributeValueMap).build());
        }

        return AttributeValue.builder().l(list).build();
    }

    @Override
    public Object transformTo(AttributeValue input) {
        List<CanonicalAddressWithResidency> list = new ArrayList<>();

        input.l()
                .forEach(
                        attributeValue -> {
                            Map<String, String> map = new HashMap<>();
                            attributeValue.m().forEach((k, v) -> map.put(k, v.s()));
                            list.add(mapper.convertValue(map, CanonicalAddressWithResidency.class));
                        });

        return list;
    }

    @Override
    public EnhancedType type() {
        return EnhancedType.listOf(CanonicalAddressWithResidency.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.L;
    }
}
