package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.peer.node.Node;
import org.nem.peer.trust.score.NodeExperience;
import org.nem.peer.trust.score.NodeExperiencePair;

import java.security.SecureRandom;
import java.util.*;

/**
 * A basic node selector implementation.
 */
public class BasicNodeSelector implements NodeSelector {

	private final TrustContext context;
	private final ColumnVector trustVector;
	private final Random random;

	/**
	 * Creates a new basic node selector.
	 *
	 * @param trustProvider The trust context.
	 */
	public BasicNodeSelector(final TrustProvider trustProvider, final TrustContext context) {
		this(trustProvider, context, new SecureRandom());
	}

	/**
	 * Creates a new basic node selector using a custom random number generator.
	 *
	 * @param trustProvider The trust context.
	 */
	public BasicNodeSelector(
			final TrustProvider trustProvider,
			final TrustContext context,
			final Random random) {
		this.context = context;
		this.trustVector = trustProvider.computeTrust(context);
		this.trustVector.normalize();
		this.random = random;
	}

	@Override
	public List<NodeExperiencePair> selectNodes(final int maxNodes) {
		final Node localNode = this.context.getLocalNode();
		final Node[] nodes = this.context.getNodes();
		final boolean[] usedNodes = new boolean[nodes.length];
		final List<NodeExperiencePair> nodePairs = new ArrayList<>();

		int numSelectedNodes;
		do {
			numSelectedNodes = nodePairs.size();

			double sum = 0;
			double rand = this.random.nextDouble();
			for (int i = 0; i < nodes.length; ++i) {
				double trust = this.trustVector.getAt(i);
				sum += trust;

				// skip nodes with zero trust and those that have already been used
				if (0 == trust || usedNodes[i] || sum < rand)
					continue;

				usedNodes[i] = true;
				final NodeExperience experience = this.context.getNodeExperiences().getNodeExperience(localNode, nodes[i]);
				nodePairs.add(new NodeExperiencePair(nodes[i], experience));
				break;
			}

			// stop the loop if either maxNodes have been selected or the last iteration didn't select a node
		} while (nodePairs.size() != maxNodes && nodePairs.size() != numSelectedNodes);

		return nodePairs;
	}
}
