package org.nem.nis.sync;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.peer.connect.SyncConnector;

/**
 * A BlockLookup implementation that looks up blocks from a remote node.
 */
public class RemoteBlockLookupAdapter implements BlockLookup {
	private final SyncConnector connector;
	private final Node remoteNode;

	/**
	 * Creates a new remote block lookup adapter.
	 *
	 * @param connector The sync connector to use.
	 * @param node The remote node.
	 */
	public RemoteBlockLookupAdapter(final SyncConnector connector, final Node node) {
		this.connector = connector;
		this.remoteNode = node;
	}

	@Override
	public BlockChainScore getChainScore() {
		return this.connector.getChainScore(this.remoteNode);
	}

	@Override
	public Block getLastBlock() {
		return this.connector.getLastBlock(this.remoteNode);
	}

	@Override
	public Block getBlockAt(final BlockHeight height) {
		return this.connector.getBlockAt(this.remoteNode, height);
	}

	@Override
	public HashChain getHashesFrom(final BlockHeight height) {
		return this.connector.getHashesFrom(this.remoteNode, height);
	}
}
