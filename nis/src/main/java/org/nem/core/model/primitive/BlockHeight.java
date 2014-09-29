package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents a NEM block height.
 */
public class BlockHeight extends AbstractPrimitive<BlockHeight, Long> implements SerializableEntity {

	/**
	 * Value representing initial height.
	 */
	public static final BlockHeight ONE = new BlockHeight(1);

	/**
	 * Value representing max height.
	 */
	public static final BlockHeight MAX = new BlockHeight(Long.MAX_VALUE);

	/**
	 * Creates a block height.
	 *
	 * @param height The block height.
	 */
	public BlockHeight(final long height) {
		super(height, BlockHeight.class);

		if (this.getRaw() <= 0) {
			throw new IllegalArgumentException("height must be positive");
		}
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
	 * Creates a new BlockHeight by subtracting one from this height.
	 *
	 * @return The new height.
	 */
	public BlockHeight prev() {
		return new BlockHeight(this.getRaw() - 1);
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
	public long getRaw() {
		return this.getValue();
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeLong("height", this.getRaw());
	}

	//region inline serialization

	/**
	 * Writes a block height object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param height The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final BlockHeight height) {
		serializer.writeLong(label, height.getRaw());
	}

	/**
	 * Reads a block height object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static BlockHeight readFrom(final Deserializer deserializer, final String label) {
		return new BlockHeight(deserializer.readLong(label));
	}

	//endregion
}
