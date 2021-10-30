package org.nem.peer.test;

import org.nem.core.node.Node;
import org.nem.core.test.NodeUtils;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.util.*;

/**
 * A test wrapper around a TrustContext.
 */
public class TestTrustContext {

	private final Node localNode;
	private final Node[] nodes;
	private final NodeExperiences nodeExperiences;
	private final PreTrustedNodes preTrustedNodes;

	/**
	 * Creates a new test trust context.
	 */
	public TestTrustContext() {
		this.localNode = NodeUtils.createNodeWithName("local");
		this.nodes = new Node[]{
				NodeUtils.createNodeWithName("bob"), NodeUtils.createNodeWithName("alice"), NodeUtils.createNodeWithName("trudy"),
				NodeUtils.createNodeWithName("peggy"), this.localNode
		};

		this.nodeExperiences = new NodeExperiences();

		final Set<Node> preTrustedNodeSet = new HashSet<>();
		preTrustedNodeSet.add(this.nodes[0]);
		preTrustedNodeSet.add(this.nodes[3]);
		this.preTrustedNodes = new PreTrustedNodes(preTrustedNodeSet);
	}

	/**
	 * Gets the real trust context.
	 *
	 * @return The trust context.
	 */
	public TrustContext getContext() {
		return new TrustContext(this.nodes, this.localNode, this.nodeExperiences, this.preTrustedNodes, new TrustParameters());
	}

	/**
	 * Sets the number of successful and failed calls that the local node has had with the node at the specified index.
	 *
	 * @param nodeIndex The node index.
	 * @param numSuccessfulCalls The number of successful calls.
	 * @param numFailedCalls The number of failed calls.
	 */
	public void setCallCounts(final int nodeIndex, final int numSuccessfulCalls, final int numFailedCalls) {
		final NodeExperience experience = this.nodeExperiences.getNodeExperience(this.localNode, this.nodes[nodeIndex]);
		experience.successfulCalls().set(numSuccessfulCalls);
		experience.failedCalls().set(numFailedCalls);
	}
}
