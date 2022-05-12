package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.*;

/**
 * A basic node selector implementation.
 */
public class BasicNodeSelector implements NodeSelector {
	private final int maxNodes;
	private final Node[] nodes;
	private final ColumnVector trustVector;
	private final Random random;

	/**
	 * Creates a new basic node selector.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param trustVector The trust vector.
	 * @param nodes All known nodes.
	 */
	public BasicNodeSelector(final int maxNodes, final ColumnVector trustVector, final Node[] nodes) {
		this(maxNodes, trustVector, nodes, new SecureRandom());
	}

	/**
	 * Creates a new basic node selector using a custom random number generator.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param trustVector The trust vector.
	 * @param nodes All known nodes.
	 * @param random The random number generator.
	 */
	public BasicNodeSelector(final int maxNodes, final ColumnVector trustVector, final Node[] nodes, final Random random) {
		this.maxNodes = maxNodes;
		this.nodes = nodes;
		this.trustVector = trustVector;
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

	private List<Node> selectNodes(final int maxNodes) {
		final List<NodeTrustPair> pairs = this.filterNodes(this.nodes, this.trustVector);
		if (maxNodes >= pairs.size()) {
			return pairs.stream().map(p -> p.node).collect(Collectors.toList());
		}

		final boolean[] usedNodes = new boolean[pairs.size()];
		final List<Node> partnerNodes = new ArrayList<>();

		int numSelectedNodes;
		double remainingTrust = pairs.stream().map(p -> p.trust).reduce(0.0, Double::sum);
		do {
			numSelectedNodes = partnerNodes.size();

			double sum = 0;
			final double rand = this.random.nextDouble() * remainingTrust;
			for (int i = 0; i < pairs.size(); ++i) {
				// skip nodes that have already been used
				if (usedNodes[i]) {
					continue;
				}

				final double trust = pairs.get(i).trust;
				sum += trust;
				if (sum < rand) {
					continue;
				}

				usedNodes[i] = true;
				remainingTrust -= trust;
				partnerNodes.add(pairs.get(i).node);
				break;
			}

			// stop the loop if either maxNodes have been selected or the last iteration didn't select a node
		} while (partnerNodes.size() != maxNodes && partnerNodes.size() != numSelectedNodes);

		return partnerNodes;
	}

	private List<NodeTrustPair> filterNodes(final Node[] nodes, final ColumnVector trustVector) {
		return IntStream.range(0, nodes.length).filter(i -> 0.0 != trustVector.getAt(i)).filter(i -> this.isCandidate(nodes[i]))
				.mapToObj(i -> new NodeTrustPair(nodes[i], trustVector.getAt(i))).collect(Collectors.toList());
	}

	protected boolean isCandidate(final Node node) {
		return true;
	}

	private static class NodeTrustPair {
		private final Node node;
		private final double trust;

		public NodeTrustPair(final Node node, final double trust) {
			this.node = node;
			this.trust = trust;
		}
	}
}
