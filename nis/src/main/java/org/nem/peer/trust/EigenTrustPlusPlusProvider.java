package org.nem.peer.trust;

/**
 * Trust provider based on the EigenTrust algorithm.
 */
public class EigenTrustPlusPlusProvider implements TrustProvider {

    @Override
    public double calculateScore(final long numSuccessfulCalls, final long numFailedCalls) {
        return numSuccessfulCalls;
    }
}

// TODO: final piece that needs to be implemented for EigenTrustPlusPlus
//	@Override
//	public void computeFeedbackCredibility(Node peer, final Node[] peers) {
//		try {
//			// 1) For each peer p we know, find all peers that we and p had experience with
//			HashMap<Node, Set<Node>> commonMap = new HashMap<Node, Set<Node>>();
//			for (int i=0; i<peers.length; i++) {
//				if (peer != peers[i]) {
//					Set<Node> common = null;
//					for (int j=0; j<peers.length; j++) {
//						NodeExperience experience1 = peer.getNodeExperience(peers[j]);
//						NodeExperience experience2 = peers[i].getNodeExperience(peers[j]);
//						if (experience1 != null && experience2 != null &&
//							experience1.getSuccessfulCalls() + experience1.getFailedCalls() > 0 &&
//							experience2.getSuccessfulCalls() + experience2.getFailedCalls() > 0) {
//							if (common == null) {
//								common = new HashSet<Node>();
//							}
//							common.add(peers[j]);
//						}
//					}
//					if (common != null && common.size() > 0) {
//						commonMap.put(peers[i], common);
//					}
//				}
//			}
//
//			// 2) Compute the correlations
//			double trSum=0.0;
//			HashMap<Node, Double> sim = new HashMap<Node, Double>();
//			for (Map.Entry<Node, Set<Node>> entry : commonMap.entrySet()) {
//				trSum = 0.0;
//				for (Node node : entry.getValue()) {
//					NodeExperience experience1 = peer.getNodeExperience(node);
//					NodeExperience experience2 = entry.getKey().getNodeExperience(node);
//					double diff = experience1.getLocalTrust() * experience1.getLocalTrustSum() - experience2.getLocalTrust() * experience2.getLocalTrustSum();
//					trSum += diff * diff;
//				}
//				trSum /= entry.getValue().size();
//
//				// Original paper suggests sim = 1-Math.sqrt(trSum).
//				// This leads to values of around 0.5 for evil nodes and almost 1 for honest nodes.
//				// We get better results by taking a power of that value since (0.5)^n quickly converges to 0 for increasing n.
//				// The value n=4 is just an example which works well.
//				sim.put(entry.getKey(), Math.pow(1-Math.sqrt(trSum), 4));
//			}
//
//			// Stored the values
//			for (Map.Entry<Node, Double> entry : sim.entrySet()) {
//				peer.getNodeExperience(entry.getKey()).setFeedbackCredibility(entry.getValue());
//			}
//			peer.getNodeExperience(peer).setFeedbackCredibility(1.0);
//		} catch (Exception e) {
//			LOGGER.warning("Exception in computeFeedbackCredibility, reason: " + e.toString());
//		}
//	}