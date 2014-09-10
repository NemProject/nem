package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;

import java.security.SecureRandom;
import java.util.*;

/**
 * A basic node selector implementation.
 */
public class BasicNodeSelector implements NodeSelector {
	private final int maxNodes;
	private final TrustContext context;
	private final ColumnVector trustVector;
	private final Random random;

	/**
	 * Creates a new basic node selector.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param trustProvider The trust provider.
	 * @param context The trust context.
	 */
	public BasicNodeSelector(
			final int maxNodes,
			final TrustProvider trustProvider,
			final TrustContext context) {
		this(maxNodes, trustProvider, context, new SecureRandom());
	}

	/**
	 * Creates a new basic node selector using a custom random number generator.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param trustProvider The trust provider.
	 * @param context The trust context.
	 * @param random The random number generator.
	 */
	public BasicNodeSelector(
			final int maxNodes,
			final TrustProvider trustProvider,
			final TrustContext context,
			final Random random) {
		this.maxNodes = maxNodes;
		this.context = context;
		this.trustVector = trustProvider.computeTrust(context);
		this.trustVector.normalize();
		this.random = random;
	}

	@Override
	public Node selectNode() {
		final List<Node> nodes = this.selectNodes(1);
		return !nodes.isEmpty() ? nodes.get(0) : null;
	}

	@Override
	public List<Node> selectNodes() {
		return this.selectNodes(this.maxNodes);
	}

	protected List<Node> selectNodes(final int maxNodes) {
		final Node[] nodes = this.context.getNodes();
		final boolean[] usedNodes = new boolean[nodes.length];
		final List<Node> partnerNodes = new ArrayList<>();

		int numSelectedNodes;
		double remainingTrust = 1.0;
		do {
			numSelectedNodes = partnerNodes.size();

			double sum = 0;
			final double rand = this.random.nextDouble() * remainingTrust;
			for (int i = 0; i < nodes.length; ++i) {
				// skip nodes with zero trust and those that have already been used
				final double trust = this.trustVector.getAt(i);
				if (0 == trust || usedNodes[i] || !this.isCandidate(nodes[i])) {
					continue;
				}

				sum += trust;
				if (sum < rand) {
					continue;
				}

				usedNodes[i] = true;
				remainingTrust -= trust;
				partnerNodes.add(nodes[i]);
				break;
			}

			// stop the loop if either maxNodes have been selected or the last iteration didn't select a node
		} while (partnerNodes.size() != maxNodes && partnerNodes.size() != numSelectedNodes);

		return partnerNodes;
	}

	protected boolean isCandidate(final Node node) {
		return true;
	}
}
