package org.nem.nis.controller.requests;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.nis.BlockChainConstants;

/**
 * Request that specifies parameters when pulling data from the db.
 * It gives NIS some flexibility to pull more or less blocks from the db.
 */
public class ChainRequest implements SerializableEntity {
	private final BlockHeight height;
	private final int minBlocks;
	private final int maxTransactions;

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
		this.height = height;
		this.minBlocks = Math.min(BlockChainConstants.BLOCKS_LIMIT, Math.max(10, minBlocks));
		this.maxTransactions =
				Math.min(BlockChainConstants.TRANSACTIONS_LIMIT, Math.max(BlockChainConstants.MAX_ALLOWED_TRANSACTIONS_PER_BLOCK, maxTransactions));
	}

	/**
	 * Creates a chain request.
	 *
	 * @param height The height after which the blocks are pulled.
	 */
	public ChainRequest(final BlockHeight height) {
		this(height, BlockChainConstants.DEFAULT_NUMBER_OF_BLOCKS_TO_PULL, BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS);
	}

	/**
	 * Creates a chain request.
	 *
	 * @param height The height after which the blocks are pulled.
	 * @param minBlocks The minimum number of blocks to pull.
	 */
	public ChainRequest(final BlockHeight height, final int minBlocks) {
		this(height, minBlocks, BlockChainConstants.DEFAULT_MAXIMUM_NUMBER_OF_TRANSACTIONS);
	}

	/**
	 * Creates a chain request.
	 *
	 * @param deserializer The deserializer to use.
	 */
	public ChainRequest(final Deserializer deserializer) {
		this(BlockHeight.readFrom(deserializer, "height"), deserializer.readInt("minBlocks"), deserializer.readInt("maxTransactions"));
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

	@Override
	public void serialize(final Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		serializer.writeInt("minBlocks", this.minBlocks);
		serializer.writeInt("maxTransactions", this.maxTransactions);
	}
}
