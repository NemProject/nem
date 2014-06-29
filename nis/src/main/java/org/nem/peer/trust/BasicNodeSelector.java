package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.peer.node.Node;

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
	public List<Node> selectNodes(final int maxNodes) {
		final Node[] nodes = this.context.getNodes();
		final boolean[] usedNodes = new boolean[nodes.length];
		final List<Node> partnerNodes = new ArrayList<>();

		int numSelectedNodes;
		double remainingTrust = 1.0;
		do {
			numSelectedNodes = partnerNodes.size();

			double sum = 0;
			double rand = this.random.nextDouble() * remainingTrust;
			for (int i = 0; i < nodes.length; ++i) {
				// skip nodes with zero trust and those that have already been used
				double trust = this.trustVector.getAt(i);
				if (0 == trust || usedNodes[i])
					continue;

				sum += trust;
				if (sum < rand)
					continue;

				usedNodes[i] = true;
				remainingTrust -= trust;
				partnerNodes.add(nodes[i]);
				break;
			}

			// stop the loop if either maxNodes have been selected or the last iteration didn't select a node
		} while (partnerNodes.size() != maxNodes && partnerNodes.size() != numSelectedNodes);

		return partnerNodes;
	}
}
