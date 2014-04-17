package org.nem.core.model;

import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * Represents a NEM block height.
 */
public class BlockHeight extends AbstractPrimitive<BlockHeight> implements SerializableEntity {

	/**
	 * Value representing initial height.
	 */
	public static final BlockHeight ONE = new BlockHeight(1);

	/**
	 * Creates a block height.
	 *
	 * @param height The block height.
	 */
	public BlockHeight(long height) {
		super(height, BlockHeight.class);

		if (this.getRaw() <= 0)
			throw new InvalidParameterException("height must be positive");
	}

	/**
	 * Deserializes a block height.
	 *
	 * @param deserializer The deserializer.
	 */
	public BlockHeight(final Deserializer deserializer) {
		this(deserializer.readLong("height"));
	}

	/**
	 * Creates a new BlockHeight by adding one to this height.
	 *
	 * @return The new height.
	 */
	public BlockHeight next() {
		return new BlockHeight(this.getRaw() + 1);
	}

	/**
	 * Calculates the difference in height between this height and another height.
	 *
	 * @param height The other height.
	 * @return The difference in height.
	 */
	public long subtract(final BlockHeight height) {
		return this.getRaw() - height.getRaw();
	}

	/**
	 * Returns the underlying height.
	 *
	 * @return The underlying height.
	 */
	public long getRaw() { return this.getValue(); }

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("height", this.getRaw());
	}
}
