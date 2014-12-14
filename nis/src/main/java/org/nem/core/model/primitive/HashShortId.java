package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents a hash's short id.
 */
public class HashShortId extends AbstractPrimitive<HashShortId, Long> implements SerializableEntity {

	/**
	 * Creates a hash short id.
	 *
	 * @param hashShortId The hash's short id.
	 */
	public HashShortId(final long hashShortId) {
		super(hashShortId, HashShortId.class);
	}

	/**
	 * Deserializes a hash short id.
	 *
	 * @param deserializer The deserializer.
	 */
	public HashShortId(final Deserializer deserializer) {
		this(deserializer.readLong("hashShortId"));
	}

	/**
	 * Returns the underlying short id.
	 *
	 * @return The underlying short id.
	 */
	public long getRaw() {
		return this.getValue();
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("hashShortId", this.getRaw());
	}
}
