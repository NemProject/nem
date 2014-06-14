package org.nem.nis.controller.viewmodels;

import java.math.BigInteger;
import java.text.*;
import java.util.*;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.*;

/**
 * Debug information about a block.
 */
public class BlockDebugInfo implements SerializableEntity {

	private final BlockHeight height;
	private final Address foragerAddress;
	private final TimeInstant timestamp;
	private final BlockDifficulty difficulty;
	private final BigInteger hit;
	private final BigInteger target;
	private final int interBlockTime;
	private final List<TransactionDebugInfo> transactionDebugInfos;

	/**
	 * Creates a new block debug info.
	 *
	 * @param blockHeight The block height.
	 * @param timestamp The block timestamp.
	 * @param foragerAddress The address of the forager of the block.
	 * @param difficulty The block difficulty.
	 * @param hit The block hit.
	 * @param target The block target.
	 * @param interBlockTime The block time - parent block time.
	 */
	public BlockDebugInfo(
			final BlockHeight blockHeight,
			final TimeInstant timestamp,
			final Address foragerAddress,
			final BlockDifficulty difficulty,
			final BigInteger hit,
			final BigInteger target,
			final int interBlockTime) {
		this.height = blockHeight;
		this.foragerAddress = foragerAddress;
		this.timestamp = timestamp;
		this.difficulty = difficulty;
		this.hit = hit;
		this.target = target;
		this.interBlockTime = interBlockTime;
		this.transactionDebugInfos = new ArrayList<>();
	}
	
	/**
	 * Deserializes a block debug info object.
	 *
	 * @param deserializer The deserializer.
	 */
	public BlockDebugInfo(final Deserializer deserializer) {
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.timestamp = readTimeStringAsTimeInstant(deserializer, "timestamp");
		this.foragerAddress = Address.readFrom(deserializer, "forager");
		this.difficulty = BlockDifficulty.readFrom(deserializer, "difficulty");
		this.hit = new BigInteger(deserializer.readString("hit"));
		this.target = new BigInteger(deserializer.readString("target"));
		this.interBlockTime = deserializer.readInt("interBlockTime");
		this.transactionDebugInfos = deserializer.readObjectArray("transactions", TransactionDebugInfo::new);
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
	public TimeInstant getTimestamp() {
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
	
	/**
	 * Returns the forager's target for the block
	 *
	 * @return The target.
	 */
	public BigInteger getTarget() {
		return this.target;
	}
	
	/**
	 * Returns the time span between the block and its parent block.
	 *
	 * @return The time span.
	 */
	public int getInterBlockTime() {
		return this.interBlockTime;
	}
	
	/**
	 * Gets the transaction debug infos associated with this block debug info.
	 *
	 * @return The transaction debug infos associated with this block.
	 */
	public List<TransactionDebugInfo> getTransactionDebugInfos() {
		return this.transactionDebugInfos;
	}

	/**
	 * Adds a new transaction debug info to this block debug info.
	 *
	 * @param transactionDebugInfo The transaction debug info to add.
	 */
	public void addTransactionDebugInfo(final TransactionDebugInfo transactionDebugInfo) {
		this.transactionDebugInfos.add(transactionDebugInfo);
	}

	@Override
	public void serialize(final Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		writeTimeInstantAsTimeString(serializer, "timestamp", this.timestamp);
		Address.writeTo(serializer, "forager", this.foragerAddress);
		BlockDifficulty.writeTo(serializer, "difficulty", this.difficulty);
		serializer.writeString("hit", this.hit.toString());
		serializer.writeString("target", this.target.toString());
		serializer.writeInt("interBlockTime", this.interBlockTime);
		serializer.writeObjectArray("transactions", this.transactionDebugInfos);
	}

	// TODO: refactor this

	private static TimeInstant readTimeStringAsTimeInstant(final Deserializer deserializer, final String name) {
		try {
			final Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(deserializer.readString(name));
			return new TimeInstant(SystemTimeProvider.getTime(date.getTime()));
		}
		catch (ParseException e) {
			return TimeInstant.ZERO;
		}
	}

	private static void writeTimeInstantAsTimeString(
			final Serializer serializer,
			final String label,
			final TimeInstant timeInstant) {
		final Date date = new Date(SystemTimeProvider.getEpochTimeMillis() + timeInstant.getRawTime() * 1000);
		final String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
		serializer.writeString(label, dateString);
	}
}
