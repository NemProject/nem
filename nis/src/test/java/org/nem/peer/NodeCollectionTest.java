package org.nem.peer;

import org.junit.*;
import org.nem.core.test.*;
import org.nem.peer.test.*;

public class NodeCollectionTest {

    //region basic partitioning

     @Test
    public void multipleNodesArePartitionedCorrectly() {
        // Act:
        final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

         // Assert:
         NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ "A", "D", "F" }, new String[]{ "B", "C" });
    }

    //endregion

    //region serialization

    @Test
    public void canRoundTripNodeCollection() {
        // Arrange:
        final NodeCollection originalNodes = createNodeCollectionWithMultipleNodes();

        // Assert:
        final NodeCollection nodes = new NodeCollection(Utils.roundtripSerializableEntity(originalNodes, null));

        // Assert:
        NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ "A", "D", "F" }, new String[]{ "B", "C" });
    }

    //endregion

    //region update

    @Test(expected = NullPointerException.class)
    public void updateCannotAddNullNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();

        // Act:
        nodes.update(null, NodeStatus.ACTIVE);
    }

    @Test
    public void updateCanAddNewActiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();

        // Act:
        nodes.update(createNode("A"), NodeStatus.ACTIVE);

        // Assert:
        NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ "A" }, new String[]{ });
    }

    @Test
    public void updateCanAddNewInactiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();

        // Act:
        nodes.update(createNode("A"), NodeStatus.INACTIVE);

        // Assert:
        NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ }, new String[]{ "A" });
    }

    @Test
    public void updateDoesNotAddNewFailureNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();

        // Act:
        nodes.update(createNode("A"), NodeStatus.FAILURE);

        // Assert:
        NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ }, new String[]{ });
    }

    @Test
    public void updateCanUpdateActiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode("A"), NodeStatus.ACTIVE);

        // Act:
        nodes.update(createNode("B", 'A'), NodeStatus.ACTIVE);

        // Assert:
        NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ "B" }, new String[]{ });
    }

    @Test
    public void updateCanUpdateActiveNodeAsInactiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode("A"), NodeStatus.ACTIVE);

        // Act:
        nodes.update(createNode("B", 'A'), NodeStatus.INACTIVE);

        // Assert:
        NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ }, new String[]{ "B" });
    }

    @Test
    public void updateCanUpdateInactiveNodeAsActiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode("A"), NodeStatus.INACTIVE);

        // Act:
        nodes.update(createNode("B", 'A'), NodeStatus.ACTIVE);

        // Assert:
        NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ "B" }, new String[]{ });
    }

    @Test
    public void updateCanUpdateInactiveNode() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode("A"), NodeStatus.INACTIVE);

        // Act:
        nodes.update(createNode("B", 'A'), NodeStatus.INACTIVE);

        // Assert:
        NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ }, new String[]{ "B" });
    }

    @Test
    public void updateOnlyUpdatesMatchingNode() {
        // Arrange:
        final NodeCollection nodes = createNodeCollectionWithMultipleNodes();

        // Act:
        nodes.update(createNode("Z", 'D'), NodeStatus.INACTIVE);

        // Assert:
        NodeCollectionAssert.arePlatformsEquivalent(nodes, new String[]{ "A", "F" }, new String[]{ "B", "C", "Z" });
    }

    // TODO: add port validation too

    //endregion

    private static Node createNode(final String platform) {
        // Arrange:
        return createNode(platform, platform.charAt(0));
    }

    private static Node createNode(final String platform, final char port) {
        // Arrange:
        return new Node(new NodeEndpoint("http", "localhost", port), platform, "FooBar");
    }

    private static NodeCollection createNodeCollectionWithMultipleNodes() {
        // Arrange:
        final NodeCollection nodes = new NodeCollection();
        nodes.update(createNode("A"), NodeStatus.ACTIVE);
        nodes.update(createNode("B"), NodeStatus.INACTIVE);
        nodes.update(createNode("C"), NodeStatus.INACTIVE);
        nodes.update(createNode("D"), NodeStatus.ACTIVE);
        nodes.update(createNode("E"), NodeStatus.FAILURE);
        nodes.update(createNode("F"), NodeStatus.ACTIVE);
        return nodes;
    }
}