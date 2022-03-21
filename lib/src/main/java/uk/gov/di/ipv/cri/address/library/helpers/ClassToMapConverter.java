package uk.gov.di.ipv.cri.address.library.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClassToMapConverter<T> implements AttributeConverter<T> {
    ObjectMapper mapper;
    Class<T> persistentClass;

    public ClassToMapConverter() {
        this(null);
    }

    public ClassToMapConverter(Class<T> persistentClass) {
        this.mapper =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule())
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.persistentClass = persistentClass;
    }

    @Override
    public AttributeValue transformFrom(Object input) {

        JavaType stringType = mapper.getTypeFactory().constructType(String.class);
        JavaType objectType = mapper.getTypeFactory().constructType(Object.class);
        JavaType mapType =
                mapper.getTypeFactory().constructMapType(Map.class, stringType, objectType);

        Map<String, Object> map = new HashMap<>();
        try {
            map = mapper.readValue(mapper.writeValueAsString(input), mapType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        for (Map.Entry entry : map.entrySet()) {
            if (String.class.equals(entry.getValue().getClass())) {
                attributeValueMap.put(
                        entry.getKey().toString(),
                        AttributeValue.builder().s(entry.getValue().toString()).build());
            } else if (Integer.class.equals(entry.getValue().getClass())
                    || Double.class.equals(entry.getValue().getClass())
                    || Float.class.equals(entry.getValue().getClass())
                    || BigDecimal.class.equals(entry.getValue().getClass())
                    || Long.class.equals(entry.getValue().getClass())) {
                attributeValueMap.put(
                        entry.getKey().toString(),
                        AttributeValue.builder().n(entry.getValue().toString()).build());
            } else if (Boolean.class.equals(entry.getValue().getClass())) {
                attributeValueMap.put(
                        entry.getKey().toString(),
                        AttributeValue.builder()
                                .bool(Boolean.parseBoolean(entry.getValue().toString()))
                                .build());
            } else if (Date.class.equals(entry.getValue().getClass())) {
                Date v = (Date) entry.getValue();
                attributeValueMap.put(
                        entry.getKey().toString(),
                        AttributeValue.builder().n(String.valueOf(v.getTime())).build());
            } else if (Optional.class.equals(entry.getValue().getClass())) {
                Optional optional = (Optional) entry.getValue();
                if (optional.isPresent()) {
                    if (String.class.equals(optional.get().getClass())) {
                        attributeValueMap.put(
                                entry.getKey().toString(),
                                AttributeValue.builder().s(optional.get().toString()).build());
                    } else if (Integer.class.equals(optional.get().getClass())
                            || Double.class.equals(optional.get().getClass())
                            || Float.class.equals(optional.get().getClass())
                            || BigDecimal.class.equals(optional.get().getClass())
                            || Long.class.equals(optional.get().getClass())) {
                        attributeValueMap.put(
                                entry.getKey().toString(),
                                AttributeValue.builder().n(optional.get().toString()).build());
                    } else if (Boolean.class.equals(optional.get().getClass())) {
                        attributeValueMap.put(
                                entry.getKey().toString(),
                                AttributeValue.builder()
                                        .bool(Boolean.valueOf(optional.get().toString()))
                                        .build());
                    } else if (Date.class.equals(optional.get().getClass())) {
                        Date v = (Date) optional.get();
                        attributeValueMap.put(
                                entry.getKey().toString(),
                                AttributeValue.builder().n(String.valueOf(v.getTime())).build());
                    }
                }
            }
        }
        return AttributeValue.builder().m(attributeValueMap).build();
    }

    @Override
    public T transformTo(AttributeValue attributeValue) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, AttributeValue> entry : attributeValue.m().entrySet()) {
            if (entry.getValue().bool() != null) {
                map.put(entry.getKey(), entry.getValue().bool());
            } else if (entry.getValue().n() != null) {
                map.put(entry.getKey(), entry.getValue().n());
            } else if (entry.getValue().s() != null) {
                map.put(entry.getKey(), entry.getValue().s());
            }
        }

        return (T) map;
    }

    @Override
    public EnhancedType<T> type() {
        return (EnhancedType<T>) EnhancedType.of(persistentClass.getClass());
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }
}
