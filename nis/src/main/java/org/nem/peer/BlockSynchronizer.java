package org.nem.peer;

public interface BlockSynchronizer {
	public void synchronizeNode(final PeerConnector connector, final Node node);
}
