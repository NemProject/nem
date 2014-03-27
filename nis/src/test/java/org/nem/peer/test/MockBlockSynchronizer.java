package org.nem.peer.test;

import org.nem.peer.BlockSynchronizer;
import org.nem.peer.Node;
import org.nem.peer.PeerConnector;

public class MockBlockSynchronizer implements BlockSynchronizer {
	@Override
	public void synchronizeNode(PeerConnector connector, Node node) {
		return;
	}
}
