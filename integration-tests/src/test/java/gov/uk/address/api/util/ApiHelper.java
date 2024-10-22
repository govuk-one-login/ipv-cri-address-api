package gov.uk.address.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

public class ApiHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static boolean validateJsonAgainstSchema(String json, String jsonSchema) {
        JsonNode jsonNode;
        JsonNode jsonSchemaNode;

        try {
            jsonNode = OBJECT_MAPPER.readTree(json);
            jsonSchemaNode = OBJECT_MAPPER.readTree(jsonSchema);
        } catch (JsonProcessingException e) {
            return false;
        }

        try {
            JSONObject jsonSchemaObject = new JSONObject(jsonSchemaNode.toString());
            Schema schema = SchemaLoader.load(jsonSchemaObject);
            schema.validate(new JSONObject(jsonNode.toString()));
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }
}
