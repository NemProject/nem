package org.nem.peer.trust;

import org.junit.*;
import org.nem.peer.*;
import org.nem.peer.test.NodeCollectionAssert;

public class TrustUtilsTest {

    @Test
    public void allNodesCanBeFlattenedIntoSingleArray() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode(87), NodeStatus.ACTIVE);
        nodes.update(createNode(82), NodeStatus.ACTIVE);
        nodes.update(createNode(86), NodeStatus.INACTIVE);
        nodes.update(createNode(84), NodeStatus.INACTIVE);
        nodes.update(createNode(81), NodeStatus.ACTIVE);

        final Node localNode = createNode(90);

        // Act:
        final Node[] nodeArray = TrustUtils.toNodeArray(nodes, localNode);

        // Assert:
        NodeCollectionAssert.arePortsEquivalent(nodeArray, new Integer[] { 81, 82, 84, 86, 87, 90 });
    }

    private static Node createNode(int port) {
        return new Node(new NodeEndpoint("http", "localhost", port), "P", "A");
    }
}
