package org.nem.core.model;

import java.math.BigInteger;

import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

/**
 * Debug information about a block.
 */
public class BlockDebugInfo implements SerializableEntity {

	private final BlockHeight height;
	private final Address foragerAddress;
	private final TimeInstant timestamp;
	private final BlockDifficulty difficulty;
	private final BigInteger hit;

	/**
	 * Creates a new block debug info.
	 *
	 * @param blockHeight The block height.
	 * @param foragerAddress The address of the forager of the block.
	 * @param timestamp The block timestamp.
	 * @param difficulty The block difficulty.
	 * @param hit The block hit.
	 */
	public BlockDebugInfo(
			final BlockHeight blockHeight,
			final Address foragerAddress,
			final TimeInstant timestamp,
			final BlockDifficulty difficulty,
			final BigInteger hit) {
		this.height = blockHeight;
		this.foragerAddress = foragerAddress;
		this.timestamp = timestamp;
		this.difficulty = difficulty;
		this.hit = hit;
	}
	
	/**
	 * Deserializes a block debug info object.
	 *
	 * @param deserializer The deserializer.
	 */
	public BlockDebugInfo(final Deserializer deserializer) {
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.foragerAddress = Address.readFrom(deserializer, "foragerAddress");
		this.timestamp = TimeInstant.readFrom(deserializer, "timestamp");
		this.difficulty = BlockDifficulty.readFrom(deserializer, "difficulty");
		this.hit = new BigInteger(deserializer.readString("hit"));
	}

	/**
	 * Returns the height of the block.
	 *
	 * @return the height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}
	
	/**
	 * Returns the address of the forager of the block.
	 *
	 * @return The address.
	 */
	public Address getForagerAddress() {
		return this.foragerAddress;
	}
	
	/**
	 * Returns the timestamp of the block.
	 *
	 * @return The timestamp.
	 */
	public TimeInstant getTimeInstant() {
		return this.timestamp;
	}
	
	/**
	 * Returns the difficulty of the block.
	 *
	 * @return The difficulty
	 */
	public BlockDifficulty getDifficulty() {
		return this.difficulty;
	}
	
	/**
	 * Returns The hit for the block
	 *
	 * @return The hit.
	 */
	public BigInteger getHit() {
		return this.hit;
	}
	
	@Override
	public void serialize(Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		Address.writeTo(serializer, "foragerAddress", foragerAddress);
		TimeInstant.writeTo(serializer, "timestamp", this.timestamp);
		BlockDifficulty.writeTo(serializer, "difficulty", this.difficulty);
		serializer.writeString("hit", this.hit.toString());
	}
}
