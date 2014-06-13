package org.nem.nis.controller.viewmodels;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockDifficulty;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeInstant;

/**
 * Debug information about a block.
 */
public class BlockDebugInfo implements SerializableEntity {

	private final BlockHeight height;
	private final Address forager;
	private TimeInstant timestamp;
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
			final Address forager,
			final BlockDifficulty difficulty,
			final BigInteger hit,
			final BigInteger target,
			final int interBlockTime) {
		this.height = blockHeight;
		this.forager = forager;
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
	 * @throws ParseException 
	 */
	public BlockDebugInfo(final Deserializer deserializer) throws ParseException {
		this.height = BlockHeight.readFrom(deserializer, "height");
		try {
			Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(deserializer.readString("timestamp"));
			this.timestamp = new TimeInstant(SystemTimeProvider.getTime(date.getTime()));
		}
		catch (ParseException e) {
			this.timestamp = new TimeInstant(0);
		}
		this.forager = Address.readFrom(deserializer, "forager");
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
	public Address getForager() {
		return this.forager;
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
	 * Returns The forager's target for the block
	 *
	 * @return The target.
	 */
	public BigInteger getTarget() {
		return this.target;
	}
	
	/**
	 * Returns the time span between the block and its parent block.
	 *
	 * @return The message.
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
		Date date = new Date(SystemTimeProvider.getEpochTimeMillis() + this.timestamp.getRawTime() * 1000);
		String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
		serializer.writeString("timestamp", dateString);
		Address.writeTo(serializer, "forager", forager);
		BlockDifficulty.writeTo(serializer, "difficulty", this.difficulty);
		serializer.writeString("hit", this.hit.toString());
		serializer.writeString("target", this.target.toString());
		serializer.writeInt("interBlockTime", this.interBlockTime);
		serializer.writeObjectArray("transactions", this.transactionDebugInfos);
	}
}