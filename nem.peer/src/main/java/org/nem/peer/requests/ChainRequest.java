package org.nem.peer.requests;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * Request that specifies parameters when pulling data from the db.
 * It gives NIS some flexibility to pull more or less blocks from the db.
 */
public class ChainRequest implements SerializableEntity {
	private final BlockHeight height;
	private final int minBlocks;
	private final int maxTransactions;

	private ChainRequest(
			final BlockHeight height,
			final Integer minBlocks,
			final Integer maxTransactions) {
		this.height = height;
		this.minBlocks = clampMinBlocks(minBlocks);
		this.maxTransactions = clampMinTransactions(maxTransactions);
	}

	/**
	 * Creates a chain request.
	 *
	 * @param height The height after which the blocks are pulled.
	 * @param minBlocks The minimum number of blocks to pull.
	 * @param maxTransactions The maximum number of transactions inside the blocks.
	 */
	public ChainRequest(
			final BlockHeight height,
			final int minBlocks,
			final int maxTransactions) {
		this(height, Integer.valueOf(minBlocks), Integer.valueOf(maxTransactions));
	}

	/**
	 * Creates a chain request.
	 *
	 * @param height The height after which the blocks are pulled.
	 */
	public ChainRequest(final BlockHeight height) {
		this(height, null, null);
	}

	/**
	 * Creates a chain request.
	 *
	 * @param height The height after which the blocks are pulled.
	 * @param minBlocks The minimum number of blocks to pull.
	 */
	public ChainRequest(final BlockHeight height, final int minBlocks) {
		this(height, minBlocks, null);
	}

	/**
	 * Creates a chain request.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public ChainRequest(final Deserializer deserializer) {
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.minBlocks = clampMinBlocks(deserializer.readOptionalInt("minBlocks"));
		this.maxTransactions = clampMinTransactions(deserializer.readOptionalInt("maxTransactions"));
	}

	private static int clampMinBlocks(final Integer value) {
		return null == value
				? BlockChainConstants.DEFAULT_NUMBER_OF_BLOCKS_TO_PULL
				: Math.min(BlockChainConstants.BLOCKS_LIMIT, Math.max(10, value));
	}

	private static int clampMinTransactions(final Integer value) {
		return null == value
				? BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS
				: Math.min(BlockChainConstants.TRANSACTIONS_LIMIT, Math.max(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, value));
	}

	/**
	 * Gets the height of the request.
	 *
	 * @return The height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Gets the minimum number of blocks.
	 *
	 * @return The minimum number of blocks.
	 */
	public int getMinBlocks() {
		return this.minBlocks;
	}

	/**
	 * Gets the maximum number of transactions.
	 *
	 * @return The maximum number of transactions.
	 */
	public int getMaxTransactions() {
		return this.maxTransactions;
	}

	/**
	 * Gets the number of blocks which should be used in the database query.
	 *
	 * @return The number of blocks.
	 */
	public int getNumBlocks() {
		return Math.min(BlockChainConstants.BLOCKS_LIMIT, this.minBlocks + 100);
	}

	@Override
	public void serialize(final Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		serializer.writeInt("minBlocks", this.minBlocks);
		serializer.writeInt("maxTransactions", this.maxTransactions);
	}
}
