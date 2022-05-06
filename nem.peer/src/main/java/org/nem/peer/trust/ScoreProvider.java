package org.nem.peer.trust;

import org.nem.core.node.Node;
import org.nem.peer.trust.score.NodeExperience;

/**
 * Interface for a trust score provider.
 */
public interface ScoreProvider {

	/**
	 * Calculates a trust score given a NodeExperience.
	 *
	 * @param experience The node experience.
	 * @return The trust score.
	 */
	double calculateTrustScore(final NodeExperience experience);

	/**
	 * Calculates a credibility score given three nodes based on the shared experiences of the first two nodes with a common node.
	 *
	 * @param node1 The first node.
	 * @param node2 The second node.
	 * @param node3 The node that the other two nodes have both interacted with.
	 * @return The mutual credibility score.
	 */
	double calculateCredibilityScore(final Node node1, final Node node2, final Node node3);
}
