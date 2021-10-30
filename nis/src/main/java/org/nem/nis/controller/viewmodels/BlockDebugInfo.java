package org.nem.nis.controller.viewmodels;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.*;

import java.math.BigInteger;
import java.util.*;

/**
 * Debug information about a block.
 */
public class BlockDebugInfo implements SerializableEntity {

	private final BlockHeight height;
	private final Address harvesterAddress;
	private final TimeInstant timeStamp;
	private final BlockDifficulty difficulty;
	private final BigInteger hit;
	private final BigInteger target;
	private final int interBlockTime;
	private final List<TransactionDebugInfo> transactionDebugInfos;

	/**
	 * Creates a new block debug info.
	 *
	 * @param blockHeight The block height.
	 * @param timeStamp The block timestamp.
	 * @param harvesterAddress The address of the harvester of the block.
	 * @param difficulty The block difficulty.
	 * @param hit The block hit.
	 * @param target The block target.
	 * @param interBlockTime The block time - parent block time.
	 */
	public BlockDebugInfo(final BlockHeight blockHeight, final TimeInstant timeStamp, final Address harvesterAddress,
			final BlockDifficulty difficulty, final BigInteger hit, final BigInteger target, final int interBlockTime) {
		this.height = blockHeight;
		this.harvesterAddress = harvesterAddress;
		this.timeStamp = timeStamp;
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
		this.timeStamp = readTimeStringAsTimeInstant(deserializer, "timeStamp");
		this.harvesterAddress = Address.readFrom(deserializer, "harvester");
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
	 * Returns the address of the harvester of the block.
	 *
	 * @return The address.
	 */
	public Address getHarvesterAddress() {
		return this.harvesterAddress;
	}

	/**
	 * Returns the timestamp of the block.
	 *
	 * @return The timestamp.
	 */
	public TimeInstant getTimeStamp() {
		return this.timeStamp;
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
	 * Returns the harvester's target for the block
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
		writeTimeInstantAsTimeString(serializer, "timeStamp", this.timeStamp);
		Address.writeTo(serializer, "harvester", this.harvesterAddress);
		BlockDifficulty.writeTo(serializer, "difficulty", this.difficulty);
		serializer.writeString("hit", this.hit.toString());
		serializer.writeString("target", this.target.toString());
		serializer.writeInt("interBlockTime", this.interBlockTime);
		serializer.writeObjectArray("transactions", this.transactionDebugInfos);
	}

	private static TimeInstant readTimeStringAsTimeInstant(final Deserializer deserializer, final String name) {
		return UnixTime.fromDateString(deserializer.readString(name), TimeInstant.ZERO).getTimeInstant();
	}

	private static void writeTimeInstantAsTimeString(final Serializer serializer, final String label, final TimeInstant timeInstant) {
		serializer.writeString(label, UnixTime.fromTimeInstant(timeInstant).getDateString());
	}
}
