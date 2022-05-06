package org.nem.peer.requests;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;

/**
 * Request that specifies parameters when pulling data from the db. It gives NIS some flexibility to pull more or less blocks from the db.
 */
public class ChainRequest implements SerializableEntity {
	private final BlockHeight height;
	private final int minBlocks;
	private final int maxTransactions;
	private final int numBlocks;

	private ChainRequest(final BlockHeight height, final Integer minBlocks, final Integer maxTransactions) {
		this(height, minBlocks, maxTransactions, NemGlobals.getBlockChainConfiguration());
	}

	private ChainRequest(final BlockHeight height, final Integer minBlocks, final Integer maxTransactions,
			final BlockChainConfiguration configuration) {
		this.height = height;
		this.minBlocks = clampMinBlocks(configuration, minBlocks);
		this.maxTransactions = clampMinTransactions(configuration, maxTransactions);
		this.numBlocks = clampNumBlocks(configuration, this.minBlocks);
	}

	/**
	 * Creates a chain request.
	 *
	 * @param height The height after which the blocks are pulled.
	 * @param minBlocks The minimum number of blocks to pull.
	 * @param maxTransactions The maximum number of transactions inside the blocks.
	 */
	public ChainRequest(final BlockHeight height, final int minBlocks, final int maxTransactions) {
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
		this(NemGlobals.getBlockChainConfiguration(), deserializer);
	}

	private ChainRequest(final BlockChainConfiguration configuration, final Deserializer deserializer) {
		this.height = BlockHeight.readFrom(deserializer, "height");
		this.minBlocks = clampMinBlocks(configuration, deserializer.readOptionalInt("minBlocks"));
		this.maxTransactions = clampMinTransactions(configuration, deserializer.readOptionalInt("maxTransactions"));
		this.numBlocks = clampNumBlocks(configuration, this.minBlocks);
	}

	private static int clampMinBlocks(final BlockChainConfiguration configuration, final Integer value) {
		return null == value
				? configuration.getDefaultBlocksPerSyncAttempt()
				: Math.min(configuration.getMaxBlocksPerSyncAttempt(), Math.max(10, value));
	}

	private static int clampMinTransactions(final BlockChainConfiguration configuration, final Integer value) {
		return null == value
				? configuration.getDefaultTransactionsPerSyncAttempt()
				: Math.min(configuration.getMaxTransactionsPerSyncAttempt(),
						Math.max(configuration.getMinTransactionsPerSyncAttempt(), value));
	}

	private static int clampNumBlocks(final BlockChainConfiguration configuration, final Integer value) {
		return Math.min(configuration.getMaxBlocksPerSyncAttempt(), value + 100);
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
		return this.numBlocks;
	}

	@Override
	public void serialize(final Serializer serializer) {
		BlockHeight.writeTo(serializer, "height", this.height);
		serializer.writeInt("minBlocks", this.minBlocks);
		serializer.writeInt("maxTransactions", this.maxTransactions);
	}
}
