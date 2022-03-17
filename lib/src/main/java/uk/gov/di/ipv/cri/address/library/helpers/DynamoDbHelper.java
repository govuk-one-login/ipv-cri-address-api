package uk.gov.di.ipv.cri.address.library.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;

public class DynamoDbHelper {

    public static class CanonicalAddressWithResidencyConverter
            implements AttributeConverter<CanonicalAddressWithResidency> {
        private final AttributeConverter<CanonicalAddressWithResidency> instance;
        private final ObjectMapper mapper;

        public CanonicalAddressWithResidencyConverter() {

            instance = new CanonicalAddressWithResidencyConverter();
            mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
        }

        @Override
        public AttributeValue transformFrom(CanonicalAddressWithResidency input) {
            String json = null;
            try {
                json = mapper.writeValueAsString(input);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            return AttributeValue.builder().s(json).build();
        }

        @Override
        public CanonicalAddressWithResidency transformTo(AttributeValue input) {
            try {
                return mapper.readValue(input.s(), CanonicalAddressWithResidency.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public EnhancedType<CanonicalAddressWithResidency> type() {
            return instance.type();
        }

        @Override
        public AttributeValueType attributeValueType() {
            return instance.attributeValueType();
        }
    }
}
