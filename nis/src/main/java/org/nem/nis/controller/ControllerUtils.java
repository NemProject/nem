package org.nem.nis.controller;

import org.nem.core.serialization.*;

public class ControllerUtils {

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
