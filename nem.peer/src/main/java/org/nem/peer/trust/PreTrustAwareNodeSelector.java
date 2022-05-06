package org.nem.peer.trust;

import org.nem.core.node.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Node selector that is aware of pre-trusted nodes.
 */
public class PreTrustAwareNodeSelector implements NodeSelector {
	private final NodeSelector selector;
	private final NodeCollection nodes;
	private final TrustContext context;
	private final Random random;

	/**
	 * Creates a new node selector.
	 *
	 * @param selector The wrapped node selector.
	 * @param nodes The node collection.
	 * @param context The trust context.
	 * @param random The random number generator.
	 */
	public PreTrustAwareNodeSelector(final NodeSelector selector, final NodeCollection nodes, final TrustContext context,
			final Random random) {
		this.selector = selector;
		this.nodes = nodes;
		this.context = context;
		this.random = random;
	}

	@Override
	public Node selectNode() {
		final Node node = this.selector.selectNode();
		if (null != node) {
			return node;
		}

		return this.selectRandom(this.getAdditionalPreTrustedNodes());
	}

	@Override
	public List<Node> selectNodes() {
		final Set<Node> nodes = new HashSet<>(this.selector.selectNodes());
		nodes.addAll(this.getAdditionalPreTrustedNodes());
		return new ArrayList<>(nodes);
	}

	private List<Node> getAdditionalPreTrustedNodes() {
		final List<Node> onlinePreTrustedNodes = this.getOnlinePreTrustedNodes();
		// BR: if all pre-trusted nodes are offline, include all of them because the network is starving
		if (onlinePreTrustedNodes.isEmpty()) {
			return new ArrayList<>(this.context.getPreTrustedNodes().getNodes());
		}

		if (this.isPreTrusted()) {
			return onlinePreTrustedNodes;
		}

		return Collections.singletonList(this.selectRandom(onlinePreTrustedNodes));
	}

	private List<Node> getOnlinePreTrustedNodes() {
		return this.context.getPreTrustedNodes().getNodes().stream()
				.filter(node -> NodeStatus.ACTIVE == this.nodes.getNodeStatus(node) && !node.equals(this.context.getLocalNode()))
				.collect(Collectors.toList());
	}

	private boolean isPreTrusted() {
		return this.context.getPreTrustedNodes().isPreTrusted(this.context.getLocalNode());
	}

	private Node selectRandom(final List<Node> nodes) {
		final int index = (int) (this.random.nextDouble() * nodes.size());
		return nodes.get(index);
	}
}
