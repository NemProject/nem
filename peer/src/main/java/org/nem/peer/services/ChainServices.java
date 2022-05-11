package org.nem.peer.services;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.Node;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Interface containing higher-level functions around accessing information about the NIS block chain of other nodes.
 */
public interface ChainServices {

	/**
	 * Gets a value indicating whether or not the local chain is synchronized with respect to the specified nodes.
	 *
	 * @param nodes The nodes.
	 * @return true if the local chain is synchronized, false otherwise.
	 */
	CompletableFuture<Boolean> isChainSynchronized(final Collection<Node> nodes);

	/**
	 * Gets the maximum block chain height for the specified NIS nodes.
	 *
	 * @param nodes The nodes.
	 * @return The maximum block chain height.
	 */
	CompletableFuture<BlockHeight> getMaxChainHeightAsync(final Collection<Node> nodes);
}
