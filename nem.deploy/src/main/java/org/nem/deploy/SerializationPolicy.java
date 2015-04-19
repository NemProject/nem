package org.nem.deploy;

import org.nem.core.serialization.*;
import org.springframework.http.MediaType;

import java.io.InputStream;

/**
 * Represents a serialization policy.
 */
public interface SerializationPolicy {

	/**
	 * The media type that the serialization policy supports.
	 *
	 * @return The supported media type.
	 */
	MediaType getMediaType();

	/**
	 * Converts the specified serializable entity to a byte representation.
	 *
	 * @param entity The entity.
	 * @return The bytes.
	 */
	byte[] toBytes(final SerializableEntity entity);

	/**
	 * Creates a deserializer from a stream.
	 *
	 * @param stream The input stream.
	 * @return The deserializer.
	 */
	Deserializer fromStream(final InputStream stream);
}
