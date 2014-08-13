package org.nem.peer.trust.score;

import org.nem.core.node.Node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A collection of TrustScore objects.
 */
public class TrustScores extends Scores<TrustScore> {

	private final Map<Node, RealDouble> trustScoreSums = new ConcurrentHashMap<>();

	@Override
	protected TrustScore createScore() {
		return new TrustScore();
	}

	/**
	 * Gets the local trust weight for the specified node.
	 *
	 * @param node The node.
	 * @return The local trust weight.
	 */
	public RealDouble getScoreWeight(final Node node) {
		RealDouble sum = this.trustScoreSums.get(node);
		if (null == sum) {
			sum = new RealDouble(0);
			this.trustScoreSums.put(node, sum);
		}

		return sum;
	}
}
