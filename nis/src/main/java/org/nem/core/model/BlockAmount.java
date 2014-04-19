package org.nem.core.model;

import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

/**
 * Represents number of blocks foraged by an account.
 */
public class BlockAmount extends AbstractPrimitive<BlockAmount> implements SerializableEntity {
	/**
	 * Value representing initial height.
	 */
	public static final BlockAmount ZERO = new BlockAmount(0);


	/**
	 * Creates a block amount.
	 *
	 * @param amount The block height.
	 */
	public BlockAmount(long amount) {
		super(amount, BlockAmount.class);

		if (this.getRaw() < 0)
			throw new IllegalArgumentException("height must be positive");
	}

	/**
	 * Deserializes a block amount.
	 *
	 * @param deserializer The deserializer.
	 */
	public BlockAmount(final Deserializer deserializer) {
		this(deserializer.readLong("amount"));
	}

	/**
	 * Creates a new BlockAmount by adding one to this amount.
	 *
	 * @return The new amount.
	 */
	public BlockAmount increment() {
		return new BlockAmount(getValue() + 1);
	}

	/**
	 * Creates a new BlockAmount by subtracting one from this amount.
	 *
	 * @return The new amount.
	 */
	public BlockAmount decrement() {
		if (getValue() == 0) {
			throw new IllegalArgumentException("decrement would make amount negative");
		}
		return new BlockAmount(getValue() - 1);
	}

	/**
	 * Returns the underlying amount.
	 *
	 * @return The underlying amount.
	 */
	public long getRaw() { return this.getValue(); }

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("amount", this.getRaw());
	}

	/**
	 * Writes a block amount object.
	 *
	 * @param serializer The serializer to use.
	 * @param label      The optional label.
	 * @param amount     The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final BlockAmount amount) {
		serializer.writeLong(label, amount.getRaw());
	}

	/**
	 * Reads a block amount object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label        The optional label.
	 *
	 * @return The read object.
	 */
	public static BlockAmount readFrom(final Deserializer deserializer, final String label) {
		return new BlockAmount(deserializer.readLong(label));
	}
}
