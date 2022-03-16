package uk.gov.di.ipv.cri.address.library.helpers;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.Optional;

public class OptionalDateConverter implements AttributeConverter {

    @Override
    public AttributeValue transformFrom(Object input) {
        Optional<Date> value = (Optional<Date>) input;
        if (value.isEmpty()) {
            return AttributeValue.builder().s(null).build();
        } else {
            return AttributeValue.builder().s(String.valueOf(value.get().getTime())).build();
        }
    }

    @Override
    public Object transformTo(AttributeValue input) {
        if (input.s() == null) {
            return Optional.empty();
        } else {
            return Optional.of(new Date(Long.parseLong(input.s())));
        }
    }

    @Override
    public EnhancedType type() {
        return EnhancedType.of(Optional.of(Date.class).getClass());
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
