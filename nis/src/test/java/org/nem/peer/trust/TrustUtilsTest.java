package org.nem.peer.trust;

import org.junit.*;
import org.nem.peer.node.*;
import org.nem.peer.test.PeerUtils;
import org.nem.peer.test.NodeCollectionAssert;

public class TrustUtilsTest {

	@Test
	public void allNodesCanBeFlattenedIntoSingleArray() {
		// Arrange:
		final NodeCollection nodes = new NodeCollection();
		nodes.update(PeerUtils.createNodeWithPort(87), NodeStatus.ACTIVE);
		nodes.update(PeerUtils.createNodeWithPort(82), NodeStatus.ACTIVE);
		nodes.update(PeerUtils.createNodeWithPort(86), NodeStatus.INACTIVE);
		nodes.update(PeerUtils.createNodeWithPort(84), NodeStatus.INACTIVE);
		nodes.update(PeerUtils.createNodeWithPort(81), NodeStatus.ACTIVE);

		final Node localNode = PeerUtils.createNodeWithPort(90);

		// Act:
		final Node[] nodeArray = TrustUtils.toNodeArray(nodes, localNode);

		// Assert:
		NodeCollectionAssert.arePortsEquivalent(nodeArray, new Integer[] { 81, 82, 84, 86, 87, 90 });
	}
}