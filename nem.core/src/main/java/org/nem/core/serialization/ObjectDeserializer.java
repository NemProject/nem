package org.nem.core.serialization;

/**
 * An interface for activating an object.
 */
public interface ObjectDeserializer<T> {

	/**
	 * Deserializes and creates an object of type T.
	 *
	 * @param deserializer The deserializer.
	 * @return The activated object.
	 */
	T deserialize(final Deserializer deserializer);
}