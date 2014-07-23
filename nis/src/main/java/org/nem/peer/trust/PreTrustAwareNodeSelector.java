package org.nem.peer.trust;

import org.nem.peer.node.*;

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
	public PreTrustAwareNodeSelector(
			final NodeSelector selector,
			final NodeCollection nodes,
			final TrustContext context,
			final Random random) {
		this.selector = selector;
		this.nodes = nodes;
		this.context = context;
		this.random = random;
	}

	@Override
	public Node selectNode() {
		return this.selector.selectNode();
	}

	@Override
	public List<Node> selectNodes() {
		final Set<Node> nodes = new HashSet<>(this.selector.selectNodes());
		nodes.addAll(this.getAdditionalPreTrustedNodes());
		return new ArrayList<>(nodes);
	}

	private List<Node> getAdditionalPreTrustedNodes() {
		final List<Node> onlinePreTrustedNodes = this.getOnlinePreTrustedNodes();
		if (0 == onlinePreTrustedNodes.size()) {
			// BR: We are starving!
			//     Refresh ALL pretrusted nodes, maybe we are lucky.
			return new ArrayList<>(this.context.getPreTrustedNodes().getNodes());
		}

		if (this.isPreTrusted())
			return onlinePreTrustedNodes;

		final int index = (int)(this.random.nextDouble() * onlinePreTrustedNodes.size());
		return Arrays.asList(onlinePreTrustedNodes.get(index));
	}

	private List<Node> getOnlinePreTrustedNodes() {
		return this.context.getPreTrustedNodes().getNodes().stream()
				.filter(node -> NodeStatus.ACTIVE  == this.nodes.getNodeStatus(node) && !node.equals(this.context.getLocalNode()))
				.collect(Collectors.toList());
	}

	private boolean isPreTrusted() {
		return this.context.getPreTrustedNodes().isPreTrusted(this.context.getLocalNode());
	}
}
