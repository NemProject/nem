package org.nem.peer.trust;

import java.util.Set;

import org.nem.peer.Node;
import org.nem.peer.PeerNetwork;

/**
 * Base class to derive from when implementing a trust class
 *
 */
public class BasicTrust implements Trust {

	@Override
	public void analyze(PeerNetwork network) {
	}

	@Override
	public void fixTrust(Node[] peers) {
	}

	@Override
	public double[] computePretrust(Node[] peers, Set<String> initialPeerAddr) {
		return null;
	}

	@Override
	public void computeLocalTrust(Node peer, Set<String> initialPeerAddr) {
	}

	@Override
	public void computeFeedbackCredibility(Node peer, Node[] peers) {
	}

	@Override
	public double[] computeGlobalTrust() {
		return null;
	}
}
