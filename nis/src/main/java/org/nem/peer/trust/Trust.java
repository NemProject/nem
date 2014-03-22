package org.nem.peer.trust;

import java.util.Set;

import org.nem.peer.Node;
import org.nem.peer.PeerNetwork;

/**
 * Every trust model must implement this interface
 */
public interface Trust {
	// analyze should calculate the global trust that the local Node has in other nodes.
	public void analyze(final PeerNetwork network);
	
	// fixTrust should check all trust related data and fix anything 
	// that might give rise to problems when calculating the global trust.
	public void fixTrust(Node[] peers);
	
	// computTrust should return the start vector for the calculation of global trust.
	// Usually only a set of well known nodes have trust values > 0 in this vector.
	public double[] computePretrust(final Node[] peers, final Set<String> initialPeerAddr);
	
	// computeLocalTrust should calculate the trust of a node based on its own experience with other nodes.
	public void computeLocalTrust(Node peer, final Set<String> initialPeerAddr);
	
	// computeFeedbackCredibilty should calculate the "felt" probability that a node returns honest feedback for other nodes.
	public void computeFeedbackCredibility(Node peer, final Node[] peers);

	// globalTrust should calculate the global trust vector for the local node of the network.
	public double[] computeGlobalTrust();
}
