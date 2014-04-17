package org.nem.nis.sync;

import org.nem.core.model.*;
import org.nem.peer.*;

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
	public Block getBlockAt(final BlockHeight height) {
		return this.connector.getBlockAt(remoteEndpoint, height);
	}

	@Override
	public HashChain getHashesFrom(final BlockHeight height) {
		return this.connector.getHashesFrom(remoteEndpoint, height);
	}
}
