package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.peer.Node;
import org.nem.peer.trust.score.NodeExperience;

/**
 * A basic node selector implementation.
 */
public class BasicNodeSelector implements NodeSelector {

	private final TrustProvider trustProvider;

	/**
	 * Creates a new basic node selector.
	 *
	 * @param trustProvider The trust provider.
	 */
	public BasicNodeSelector(final TrustProvider trustProvider) {
		this.trustProvider = trustProvider;
	}

	@Override
	public NodeExperiencePair selectNode(final TrustContext context) {
		final ColumnVector trustVector = this.trustProvider.computeTrust(context);
		trustVector.normalize();

		double sum = 0;
		double rand = Math.random();

		final Node localNode = context.getLocalNode();
		final Node[] nodes = context.getNodes();
		for (int i = 0; i < nodes.length; ++i) {
			sum += trustVector.getAt(i);
			if (sum < rand)
				continue;

			final NodeExperience experience = context.getNodeExperiences().getNodeExperience(localNode, nodes[i]);
			return new NodeExperiencePair(nodes[i], experience);
		}

		throw new TrustException("No available peers found");
	}
}
