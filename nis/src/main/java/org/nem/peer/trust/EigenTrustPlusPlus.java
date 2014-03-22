package org.nem.peer.trust;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.nem.peer.Node;
import org.nem.peer.PeerNetwork;

public class EigenTrustPlusPlus extends EigenTrust implements Trust {
	private static final Logger LOGGER = Logger.getLogger(EigenTrustPlusPlus.class.getName());

	/**
	 * Computes the global trust for the network nodes.
	 * The global trust values are stored in the localNode object of the network.
	 * 
	 * @param network    the network of nodes to analyze.
	 */
	@Override
	public void analyze(final PeerNetwork network) {
		if (network == null) {
			throw new IllegalArgumentException("Analyze requires a PeerNetwork object not equal to null as parameter.");
		}
		Node[] peers = network.getAllPeers().toArray(new Node[network.getAllPeers().size()+1]);
		peers[peers.length-1] = network.getLocalNode();

		// Compute the trust we have in other nodes due to our own experience
		computeLocalTrust(peers[peers.length-1], network.getInitialPeerAddr());
		
		// Check for corrupt data and fix it
		fixTrust(peers);
		
		// Check for corrupt data and fix it
		computeFeedbackCredibility(peers[peers.length-1], peers);
		
		// Compute pretrust vector
		pretrustVector = computePretrust(peers, network.getInitialPeerAddr());

		// Set up the transpose of the trust matrix
		boolean outputTrustMatrix = false;
		boolean outputFedbackCredibilityMatrix = false;
		trustMatrix = new double[peers.length][peers.length];
		for (int i=0; i<peers.length; i++) {
			for (int j=0; j<peers.length; j++) {
				NodeExperience experience = peers[i].getNodeExperience(peers[j]);
				trustMatrix[j][i] = experience.getLocalTrust() * experience.getFeedbackCredibility();
			}
		}
		
		// Since weighing the local trust values with feedback credibility ruins the normalization,
		// the columns are normalized again.
		normalizeColumns(trustMatrix);
		
		// In case we want to debug
		if (outputTrustMatrix) {
			outputMatrix("Trust Matrix", trustMatrix);
		}
		if (outputFedbackCredibilityMatrix) {
			outputFeedbackCredibility(peers);
		}
		
		// Calculate global trust
		double[] globalTrust = computeGlobalTrust();
		
		// You can discover the trust we have in a peer by 
		// calling getNodeExperience(address).getGlobalTrust() on the localNode object if you have the peer's address.
		for (int i=0; i<peers.length; i++) {
			NodeExperience experience = peers[peers.length-1].getNodeExperience(peers[i]);
			if (experience == null) {
				peers[peers.length-1].setNodeExperience(peers[i], new NodeExperience());
			}
			peers[peers.length-1].getNodeExperience(peers[i]).setGlobalTrust(globalTrust[i]);
		}
	}

	/**
	 * Output the feedback credibility in the console in matrix form.
	 * 
	 * @param peers     array of all peers   
	 */
	private void outputFeedbackCredibility(Node[] peers) {
		double[][] matrix = new double[peers.length][peers.length];
		for (int i=0; i<peers.length; i++) {
			for (int j=0; j<peers.length; j++) {
				NodeExperience experience = peers[i].getNodeExperience(peers[j]);
				matrix[j][i] = experience.getFeedbackCredibility();
			}
		}
		outputMatrix("Feedback Credibility Matrix", matrix);
	}
	
	/**
	 * Compute the trust a node has in other nodes due to its own experience with them.
	 */
	@Override
	public void computeLocalTrust(Node peer, final Set<String> initialPeerAddr) {
		long numCalls;
		double sum=0.0;
		HashMap<Node, Double> tmp = new HashMap<Node, Double>();
		for (Map.Entry<Node, NodeExperience> entry : peer.getNodeExperiences().entrySet()) {
			NodeExperience experience = entry.getValue();
			numCalls = experience.getSuccessfulCalls() + experience.getFailedCalls();
			if (numCalls > 0) {
				tmp.put(entry.getKey(), (double)(experience.getSuccessfulCalls())/(double)numCalls);
			}
			else {
				if (initialPeerAddr.contains(entry.getKey().getAddress()) || peer == entry.getKey()) {
					tmp.put(entry.getKey(),1.0);
				}
				else {
					tmp.put(entry.getKey(),0.0);
				}
			}
			sum += Math.abs(tmp.get(entry.getKey()));
		}
		
		for (Map.Entry<Node, NodeExperience> entry : peer.getNodeExperiences().entrySet()) {
			NodeExperience experience = entry.getValue();
			experience.setLocalTrust(tmp.get(entry.getKey())/sum);
			experience.setLocalTrustSum(sum);
		}
	}
	
	/**
	 * Compute the trust that a node has in the trust values supplied by other nodes by correlating experiences.
	 */
	@Override
	public void computeFeedbackCredibility(Node peer, final Node[] peers) {
		try {
			// 1) For each peer p we know, find all peers that we and p had experience with
			HashMap<Node, Set<Node>> commonMap = new HashMap<Node, Set<Node>>();
			for (int i=0; i<peers.length; i++) {
				if (peer != peers[i]) {
					Set<Node> common = null;
					for (int j=0; j<peers.length; j++) {
						NodeExperience experience1 = peer.getNodeExperience(peers[j]);
						NodeExperience experience2 = peers[i].getNodeExperience(peers[j]);
						if (experience1 != null && experience2 != null &&
							experience1.getSuccessfulCalls() + experience1.getFailedCalls() > 0 &&
							experience2.getSuccessfulCalls() + experience2.getFailedCalls() > 0) {
							if (common == null) {
								common = new HashSet<Node>();
							}
							common.add(peers[j]);
						}
					}
					if (common != null && common.size() > 0) {
						commonMap.put(peers[i], common);
					}
				}
			}
			
			// 2) Compute the correlations
			double trSum=0.0;
			HashMap<Node, Double> sim = new HashMap<Node, Double>();
			for (Map.Entry<Node, Set<Node>> entry : commonMap.entrySet()) {
				trSum = 0.0;
				for (Node node : entry.getValue()) {
					NodeExperience experience1 = peer.getNodeExperience(node);
					NodeExperience experience2 = entry.getKey().getNodeExperience(node);
					double diff = experience1.getLocalTrust() * experience1.getLocalTrustSum() - experience2.getLocalTrust() * experience2.getLocalTrustSum();
					trSum += diff * diff;
				}
				trSum /= entry.getValue().size();

				// Original paper suggests sim = 1-Math.sqrt(trSum).
				// This leads to values of around 0.5 for evil nodes and almost 1 for honest nodes. 
				// We get better results by taking a power of that value since (0.5)^n quickly converges to 0 for increasing n.
				// The value n=4 is just an example which works well.
				sim.put(entry.getKey(), Math.pow(1-Math.sqrt(trSum), 4));
			}
			
			// Stored the values 
			for (Map.Entry<Node, Double> entry : sim.entrySet()) {
				peer.getNodeExperience(entry.getKey()).setFeedbackCredibility(entry.getValue());
			}
			peer.getNodeExperience(peer).setFeedbackCredibility(1.0);
		} catch (Exception e) {
			LOGGER.warning("Exception in computeFeedbackCredibility, reason: " + e.toString());
		}
	}
}
