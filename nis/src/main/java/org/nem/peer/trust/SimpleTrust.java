package org.nem.peer.trust;

import java.util.Set;

import org.nem.peer.Node;
import org.nem.peer.PeerNetwork;

public class SimpleTrust extends BasicTrust implements Trust {

	/**
	 * In the simple trust model, every node trusts every other node.
	 * @param network
	 */
	@Override
	public void analyze(final PeerNetwork network) {
		if (network == null) {
			throw new IllegalArgumentException("Analyze requires a PeerNetwork object not equal to null as parameter.");
		}
		Node[] peers = network.getAllPeers().toArray(new Node[network.getAllPeers().size()+1]);
		peers[peers.length-1] = network.getLocalNode();

		for (int i=0; i<peers.length; i++) {
			for (int j=0; j<peers.length; j++) {
				NodeExperience experience = peers[i].getNodeExperience(peers[j]);
				if (experience != null) {
					experience.setLocalTrust(1.0/(double)peers.length);
				}
				else {
					experience = new NodeExperience();
					experience.setLocalTrust(1.0/(double)peers.length);
					experience.setGlobalTrust(1.0/(double)peers.length);
					peers[i].setNodeExperience(peers[j], experience);
				}
			}
		}
	}

	@Override
	public void fixTrust(Node[] peers) {
		// Do nothing
	}
	
	@Override
	public double[] computePretrust(Node[] peers, Set<String> initialPeerAddr) {
		// Do nothing
		return null;
	}

	public void computeLocalTrust(Node peer, final Set<String> initialPeerAddr) {
		// Do nothing		
	}

	@Override
	public void computeFeedbackCredibility(Node peer, final Node[] peers) {
		// Do nothing
	}
	
	@Override
	public double[] computeGlobalTrust() {
		// Do nothing
		return null;
	}
}
