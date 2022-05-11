package org.nem.peer.connect;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.peer.requests.*;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Interface that is used to sync blocks and transactions across peers.
 */
public interface SyncConnector {

	// region synchronous requests

	/**
	 * Requests information about the last block in the chain from the specified node.
	 *
	 * @param node The remote node.
	 * @return The last block.
	 */
	Block getLastBlock(final Node node);

	/**
	 * Requests information about the block at the specified height from the specified node.
	 *
	 * @param node The remote node.
	 * @param height The block height.
	 * @return The block at the specified height
	 */
	Block getBlockAt(final Node node, final BlockHeight height);

	/**
	 * Requests information about the hashes of all blocks in the chain after the specified height from the specified node.
	 *
	 * @param node The remote node.
	 * @param height The block height
	 * @return The hashes of all blocks in the chain after the specified height.
	 */
	HashChain getHashesFrom(final Node node, final BlockHeight height);

	/**
	 * Requests information about all blocks in the chain after the specified height from the specified node.
	 *
	 * @param node The remote node.
	 * @param chainRequest The chain request.
	 * @return All blocks in the chain as specified in the chain request.
	 */
	Collection<Block> getChainAfter(final Node node, final ChainRequest chainRequest);

	/**
	 * Requests information about the cumulative score of the remote chain from the specified node.
	 *
	 * @param node The remote node.
	 * @return The cumulative score for the endpoint's chain.
	 */
	BlockChainScore getChainScore(final Node node);

	/**
	 * Requests new unconfirmed transactions from the specified node.
	 *
	 * @param node The remote node.
	 * @param unconfirmedTransactionsRequest The unconfirmed transactions request.
	 * @return All new unconfirmed transactions from the endpoint.
	 */
	Collection<Transaction> getUnconfirmedTransactions(final Node node,
			final UnconfirmedTransactionsRequest unconfirmedTransactionsRequest);

	// endregion

	// region asynchronous requests

	/**
	 * Requests information about the block height of the remote chain.
	 *
	 * @param node The remote node.
	 * @return The completable future containing the block height of the remote chain.
	 */
	CompletableFuture<BlockHeight> getChainHeightAsync(final Node node);

	// endregion
}
