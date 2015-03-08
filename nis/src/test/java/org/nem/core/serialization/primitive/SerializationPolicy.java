package org.nem.core.serialization.primitive;

import org.nem.core.serialization.*;

/**
 * Serialization policy used for testing.
 *
 * @param <TSerializer> The serializer type.
 * @param <TDeserializer> The deserializer type.
 */
public abstract class SerializationPolicy<TSerializer extends Serializer, TDeserializer extends Deserializer> {

	/**
	 * Creates a default serializer to use.
	 *
	 * @return A serializer.
	 */
	public final TSerializer createSerializer() {
		return this.createSerializer(new SerializationContext());
	}

	/**
	 * Creates a default serializer to use.
	 *
	 * @param context The serialization context.
	 * @return A serializer.
	 */
	public abstract TSerializer createSerializer(final SerializationContext context);

	/**
	 * Creates a deserializer that reads from the specified serializer.
	 *
	 * @param serializer The serializer.
	 * @return A deserializer.
	 */
	public final TDeserializer createDeserializer(final TSerializer serializer) {
		return this.createDeserializer(serializer, new DeserializationContext(null));
	}

	/**
	 * Creates a deserializer that reads from the specified serializer.
	 *
	 * @param serializer The serializer.
	 * @param context The deserialization context.
	 * @return A deserializer.
	 */
	public abstract TDeserializer createDeserializer(
			final TSerializer serializer,
			final DeserializationContext context);
}
