package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.peer.node.Node;
import org.nem.peer.trust.score.NodeExperience;
import org.nem.peer.trust.score.NodeExperiencePair;

/**
 * A basic node selector implementation.
 */
public class BasicNodeSelector implements NodeSelector {

	private final TrustContext context;
	private final ColumnVector trustVector;

	/**
	 * Creates a new basic node selector.
	 *
	 * @param trustProvider The trust context.
	 */
	public BasicNodeSelector(final TrustProvider trustProvider, final TrustContext context) {
		this.context = context;
		this.trustVector = trustProvider.computeTrust(context);
		this.trustVector.normalize();
	}

	@Override
	public NodeExperiencePair selectNode() {
		double sum = 0;
		double rand = Math.random();

		final Node localNode = this.context.getLocalNode();
		final Node[] nodes = this.context.getNodes();
		for (int i = 0; i < nodes.length; ++i) {
			sum += this.trustVector.getAt(i);
			if (sum < rand)
				continue;

			final NodeExperience experience = this.context.getNodeExperiences().getNodeExperience(localNode, nodes[i]);
			return new NodeExperiencePair(nodes[i], experience);
		}

		return null;
	}
}
