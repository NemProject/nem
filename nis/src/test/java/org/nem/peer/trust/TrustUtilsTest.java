package org.nem.peer.trust;

import org.junit.*;
import org.nem.core.test.Utils;
import org.nem.peer.*;
import org.nem.peer.test.NodeCollectionAssert;

public class TrustUtilsTest {

    @Test
    public void allNodesCanBeFlattenedIntoSingleArray() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(Utils.createNodeWithPort(87), NodeStatus.ACTIVE);
        nodes.update(Utils.createNodeWithPort(82), NodeStatus.ACTIVE);
        nodes.update(Utils.createNodeWithPort(86), NodeStatus.INACTIVE);
        nodes.update(Utils.createNodeWithPort(84), NodeStatus.INACTIVE);
        nodes.update(Utils.createNodeWithPort(81), NodeStatus.ACTIVE);

        final Node localNode = Utils.createNodeWithPort(90);

        // Act:
        final Node[] nodeArray = TrustUtils.toNodeArray(nodes, localNode);

        // Assert:
        NodeCollectionAssert.arePortsEquivalent(nodeArray, new Integer[] { 81, 82, 84, 86, 87, 90 });
    }
}