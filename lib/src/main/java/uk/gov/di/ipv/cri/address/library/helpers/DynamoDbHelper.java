package uk.gov.di.ipv.cri.address.library.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import uk.gov.di.ipv.cri.address.library.models.CanonicalAddressWithResidency;

public class DynamoDbHelper {

    public class CanonicalAddressWithResidencyConverter
            implements AttributeConverter<CanonicalAddressWithResidency> {
        private AttributeConverter<CanonicalAddressWithResidency> instance;
        private final ObjectMapper mapper = new ObjectMapper();

        public CanonicalAddressWithResidencyConverter() {
            instance = new CanonicalAddressWithResidencyConverter();
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
