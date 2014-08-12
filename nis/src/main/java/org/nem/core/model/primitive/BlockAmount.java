package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents a non-negative amount of blocks.
 */
public class BlockAmount extends AbstractPrimitive<BlockAmount, Long> {

	/**
	 * Value representing initial height.
	 */
	public static final BlockAmount ZERO = new BlockAmount(0);

	/**
	 * Creates a block amount.
	 *
	 * @param amount The block height.
	 */
	public BlockAmount(final long amount) {
		super(amount, BlockAmount.class);

		if (this.getRaw() < 0) {
			throw new IllegalArgumentException("amount must be non-negative");
		}
	}

	/**
	 * Creates a new BlockAmount by adding one to this amount.
	 *
	 * @return The new amount.
	 */
	public BlockAmount increment() {
		return new BlockAmount(this.getValue() + 1);
	}

	/**
	 * Creates a new BlockAmount by subtracting one from this amount.
	 *
	 * @return The new amount.
	 */
	public BlockAmount decrement() {
		return new BlockAmount(this.getValue() - 1);
	}

	/**
	 * Returns the underlying amount.
	 *
	 * @return The underlying amount.
	 */
	public long getRaw() {
		return this.getValue();
	}

	/**
	 * Writes a block amount object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param amount The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final BlockAmount amount) {
		serializer.writeLong(label, amount.getRaw());
	}

	/**
	 * Reads a block amount object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static BlockAmount readFrom(final Deserializer deserializer, final String label) {
		return new BlockAmount(deserializer.readLong(label));
	}
}
