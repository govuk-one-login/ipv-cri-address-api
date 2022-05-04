package uk.gov.di.ipv.cri.address.library.util;

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
import uk.gov.di.ipv.cri.address.library.exception.ClassToMapException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClassToMapConverter<T> implements AttributeConverter<T> {
    private final ObjectMapper mapper;
    private final Class<T> persistentClass;

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

        Map<String, Object> map;
        try {
            map = mapper.readValue(mapper.writeValueAsString(input), mapType);
        } catch (JsonProcessingException e) {
            throw new ClassToMapException(e);
        }
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        map.forEach(
                (key, val) -> {
                    Object value = null;
                    // If our value is null, leave it out the map
                    if (val != null) {

                        String className = val.getClass().getSimpleName();

                        if (Optional.class.equals(val.getClass())) {
                            Optional<?> optional = (Optional<?>) val;
                            if (optional.isPresent()) {
                                value = optional.get();
                                className = value.getClass().getSimpleName();
                            } else {
                                className = "null";
                            }
                        } else {
                            value = val;
                        }

                        if (value != null)
                            switch (className) {
                                case "Integer":
                                case "BigDecimal":
                                case "Long":
                                case "Double":
                                case "Float":
                                    attributeValueMap.put(
                                            key,
                                            AttributeValue.builder().n(value.toString()).build());
                                    break;
                                case "Boolean":
                                    attributeValueMap.put(
                                            key,
                                            AttributeValue.builder()
                                                    .bool(Boolean.parseBoolean(value.toString()))
                                                    .build());
                                    break;
                                case "Date":
                                    Date date = (Date) value;
                                    attributeValueMap.put(
                                            key,
                                            AttributeValue.builder()
                                                    .n(String.valueOf(date.getTime()))
                                                    .build());
                                    break;
                                default:
                                    attributeValueMap.put(
                                            key,
                                            AttributeValue.builder().s(value.toString()).build());
                                    break;
                            }
                    }
                });
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

        //noinspection unchecked
        return (T) map;
    }

    @Override
    public EnhancedType<T> type() {
        return EnhancedType.of(persistentClass);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }
}
