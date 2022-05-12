package org.nem.peer.trust;

import org.nem.core.node.Node;
import org.nem.peer.trust.score.NodeExperiences;

/**
 * Contains contextual information that can be used to influence the trust computation.
 */
public class TrustContext {

	private final Node[] nodes;
	private final Node localNode;
	private final NodeExperiences nodeExperiences;
	private final PreTrustedNodes preTrustedNodes;
	private final TrustParameters params;

	/**
	 * Creates a new trust context.
	 *
	 * @param nodes The known nodes (including the local node).
	 * @param localNode The local node.
	 * @param nodeExperiences Node experiences information.
	 * @param preTrustedNodes Pre-trusted node information.
	 * @param params Additional parameters associated with the trust context.
	 */
	public TrustContext(final Node[] nodes, final Node localNode, final NodeExperiences nodeExperiences,
			final PreTrustedNodes preTrustedNodes, final TrustParameters params) {

		this.nodes = nodes;
		this.localNode = localNode;
		this.nodeExperiences = nodeExperiences;
		this.preTrustedNodes = preTrustedNodes;
		this.params = params;
	}

	/**
	 * Gets all nodes in the trust context.
	 *
	 * @return All nodes.
	 */
	public Node[] getNodes() {
		return this.nodes;
	}

	/**
	 * Gets the local node
	 *
	 * @return The local noe.
	 */
	public Node getLocalNode() {
		return this.localNode;
	}

	/**
	 * Gets all node experience information from the trust context.
	 *
	 * @return Node experience information.
	 */
	public NodeExperiences getNodeExperiences() {
		return this.nodeExperiences;
	}

	/**
	 * Gets all pre-trusted node information from the trust context.
	 *
	 * @return Pre-trusted node information.
	 */
	public PreTrustedNodes getPreTrustedNodes() {
		return this.preTrustedNodes;
	}

	/**
	 * Gets additional parameters associated with the trust context.
	 *
	 * @return Additional parameters associated with the trust context.
	 */
	public TrustParameters getParams() {
		return this.params;
	}
}
