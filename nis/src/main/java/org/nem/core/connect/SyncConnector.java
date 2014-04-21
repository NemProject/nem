package org.nem.core.connect;

import org.nem.core.connect.NodeEndpoint;
import org.nem.core.model.*;

import java.util.List;

/**
 * Interface that is used to sync blocks across peers.
 */
public interface SyncConnector {

	/**
	 * Requests information about the last block in the chain from the specified node.
	 *
	 * @param endpoint The endpoint.
	 * @return The last block.
	 */
	public Block getLastBlock(final NodeEndpoint endpoint);

	/**
	 * Requests information about the block at the specified height from the specified node.
	 *
	 * @param endpoint The endpoint.
	 * @param height The block height.
	 * @return The block at the specified height
	 */
	public Block getBlockAt(final NodeEndpoint endpoint, final BlockHeight height);

	/**
	 * Requests information about the hashes of all blocks in the chain after the specified height
	 * from the specified node.
	 *
	 * @param endpoint The endpoint.
	 * @param height The block height
	 * @return The hashes of all blocks in the chain after the specified height.
	 */
	public HashChain getHashesFrom(final NodeEndpoint endpoint, final BlockHeight height);

	/**
	 * Requests information about all blocks in the chain after the specified height
	 * from the specified node.
	 *
	 * @param endpoint The endpoint.
	 * @param height The block height.
	 * @return All blocks in the chain after the specified height.
	 */
	public List<Block> getChainAfter(final NodeEndpoint endpoint, final BlockHeight height);
}
