package uk.gov.di.ipv.cri.address.library.helpers;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Optional;

public class OptionalBooleanConverter implements AttributeConverter {
    @Override
    public AttributeValue transformFrom(Object input) {
        Optional<Boolean> value = (Optional<Boolean>) input;
        if (value.isEmpty()) {
            return AttributeValue.builder().s(null).build();
        } else {
            return AttributeValue.builder().s(value.get().toString()).build();
        }
    }

    @Override
    public Object transformTo(AttributeValue input) {
        return input.s() == null ? Optional.empty() : Optional.of(Boolean.parseBoolean(input.s()));
    }

    @Override
    public EnhancedType type() {
        return EnhancedType.of(Optional.of(Boolean.class).getClass());
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
