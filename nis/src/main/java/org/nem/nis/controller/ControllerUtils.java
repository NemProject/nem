package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

public class ControllerUtils {

    public static String serialize(final SerializableEntity entity) {
        return JsonSerializer.serializeToJson(entity).toString() + "\r\n";
    }

    public static Deserializer getDeserializer(final String jsonString, final AccountLookup accountLookup) {
        final Object result = JSONValue.parse(jsonString);
        if (result instanceof JSONObject)
            return new JsonDeserializer((JSONObject)result, new DeserializationContext(accountLookup));

        throw new InvalidParameterException(String.format("JSON Object is malformed \"%s\"", jsonString));
    }

    public static Deserializer getDeserializer(final byte[] bytes, final AccountLookup accountLookup) {
        return new BinaryDeserializer(bytes, new DeserializationContext(accountLookup));
    }
}
