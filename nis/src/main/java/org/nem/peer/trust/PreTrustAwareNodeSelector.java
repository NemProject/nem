package org.nem.peer.trust;

import org.nem.peer.node.Node;
import org.nem.peer.trust.score.NodeExperiencePair;

import java.util.List;

/**
* Node selector that is aware of pre-trusted nodes.
*/
public class PreTrustAwareNodeSelector implements NodeSelector {
	private final NodeSelector selector;
	private final TrustContext context;

	/**
	 * Creates a new node selector.
	 *
	 * @param selector The wrapped node selector.
	 * @param context The trust context.
	 */
	public PreTrustAwareNodeSelector(final NodeSelector selector, final TrustContext context) {
		this.selector = selector;
		this.context = context;
	}

	@Override
	public Node selectNode() {
		return this.selector.selectNode();
	}

	@Override
	public List<Node> selectNodes(int maxNodes) {
		return this.selector.selectNodes(maxNodes);
	}
}
