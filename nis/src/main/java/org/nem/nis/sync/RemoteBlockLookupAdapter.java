package org.nem.nis.sync;

import org.nem.core.model.Block;
import org.nem.core.model.HashChain;
import org.nem.peer.Node;
import org.nem.peer.NodeEndpoint;
import org.nem.peer.SyncConnector;

/**
 * A BlockLookup implementation that looks up blocks from a remote node.
 */
public class RemoteBlockLookupAdapter implements BlockLookup {

	private final SyncConnector connector;
	private final NodeEndpoint remoteEndpoint;

	/**
	 * Creates a new remote block lookup adapter.
	 *
	 * @param connector The sync connector to use.
	 * @param node The remote node.
	 */
	public RemoteBlockLookupAdapter(final SyncConnector connector, final Node node) {
		this.connector = connector;
		this.remoteEndpoint = node.getEndpoint();
	}

	@Override
	public Block getLastBlock() {
		return this.connector.getLastBlock(remoteEndpoint);
	}

	@Override
	public Block getBlockAt(long height) {
		return this.connector.getBlockAt(remoteEndpoint, height);
	}

	@Override
	public HashChain getHashesFrom(long height) {
		return this.connector.getHashesFrom(remoteEndpoint, height);
	}
}
