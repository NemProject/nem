package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

public class ControllerUtils {

	/**
	 * Creates a deserializer around a JSON string.
	 *
	 * @param jsonString    The JSON string.
	 * @param accountLookup The account lookup.
	 *
	 * @return A deserializer.
	 */
	public static Deserializer getDeserializer(final String jsonString, final AccountLookup accountLookup) {
		final Object result = JSONValue.parse(jsonString);
		if (result instanceof JSONObject)
			return new JsonDeserializer((JSONObject)result, new DeserializationContext(accountLookup));

		throw new InvalidParameterException(String.format("JSON Object is malformed \"%s\"", jsonString));
	}

	/**
	 * Creates a deserializer around a byte array.
	 *
	 * @param bytes         The byte array.
	 * @param accountLookup The account lookup.
	 *
	 * @return A deserializer.
	 */
	public static BinaryDeserializer getDeserializer(final byte[] bytes, final AccountLookup accountLookup) {
		return new BinaryDeserializer(bytes, new DeserializationContext(accountLookup));
	}
}
